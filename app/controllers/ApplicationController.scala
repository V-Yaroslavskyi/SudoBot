package controllers

import java.io.File
import java.sql.{Date => DateSQL}
import java.util.Date
import javax.inject.Inject

import models.Tables.{BidsRow, CommoditiesRow, CompaniesRow, ContractsRow, UsersChatsRow}
import models._
import org.asynchttpclient.request.body.multipart.{FilePart, StringPart}
import org.asynchttpclient.{AsyncCompletionHandler, AsyncHttpClient, Response}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.json4s._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods.{parse => parse4s, render => render4s, _}
import org.json4s.native.Serialization
import play.api.cache.CacheApi
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSResponse
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, AnyContent, Controller}
import processors.RecognitionHelper
import telegram._
import telegram.methods._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success, Try}

class ApplicationController @Inject()(ws: WSClient, conf: play.api.Configuration,
                                      cache: CacheApi, recognitionHelper: RecognitionHelper)
                                     (implicit val exc: ExecutionContext)
  extends Controller {


  val url = s"https://api.telegram.org/bot${conf.getString("token").get}"

  val webhookStatus: Future[Unit] = setWebhook().map { x =>
    println(x.body)
  }

  val dtf = DateTimeFormat.forPattern("dd.MM.yyyy")

  implicit val formats: Formats = Serialization.formats(NoTypeHints) +
    new EnumNameSerializer(ChatAction) +
    new EnumNameSerializer(ParseMode)

  def index: Action[AnyContent] = Action { request =>
    Ok("ok")
  }

  def ping: Action[AnyContent] = Action { request =>
    Ok("PONG")
  }

  def inbox: Action[AnyContent] = Action.async { request =>
    val js = request.body.asJson.get
    val update = fromJson[Update](js.toString)
    val response = (update.message, update.callbackQuery) match {
      case (Some(msg), None) =>
        val mode = cache.get[Int](s"contract_mode:${msg.chat.id}")
        val command = getCommand(msg)
        command match {
          case Some("/start") =>
            start(msg.chat.id)

          case Some("/suit") =>
            create_suit(msg.chat.id)

          case _ =>
            msg.replyToMessage match {
              case Some(x) if msg.text.isDefined =>
                process_reply(msg, x, mode)
              case _ =>
                Future successful Some(SendMessage(Left(msg.chat.id), "Я не понимаю этой комманды"))
            }
        }
      case (None, Some(cbq)) =>
        val callbackData = cbq.data.map(_.split(";")) flatMap {
          case Array(c, v) => Some((c, v))
          case _ => None
        }
        callbackData match {
          case Some((cbCom, cbVal)) =>
            cbCom match {
              case "marriage" => register_marriage(cbq.from.id)

              case "doc" =>
                cbVal match {
                  case "scan" => ask(cbq.from.id,
                    s"""
                       |ОК. ТОДІ ЗІСКАНУЙ МЕНІ, БУДЬ ЛАСКА, КОПІЮ СВОГО СВІДОЦТВА ПРО ШЛЮБ
                       |Ще трішки і цей документ перестане діяти :)
                    """.stripMargin, "scan")

                  case "photo" => ask(cbq.from.id, "ОК, ТОДІ СФОТКАЙ, БУДЬ ЛАСКА, СВОЄ СВІДОЦТВО ПРО ШЛЮБ", "scan")
                }
              //            }
              case _ => Future successful errorMsg(cbq.from.id)

            }
          case x =>
            println("Unknown update " + update)
            Future successful errorMsg(cbq.from.id)
        }
    }
    response.map(_.map { x => Ok(toAnswerJson(x, x.methodName)) }.getOrElse(Ok))
  }

  def start(chatId: Long): Future[Option[SendMessage]] = {
    Future successful {
      Some(
        SendMessage(Left(chatId),
          """
            |ПРИВІТ! Я СУДОБОТ.
            |Я допоможу Тобі підготувати необхідні для суду документи без юриста.
          """.stripMargin)
      )
    }
  }

  def create_suit(chatId: Long): Future[Option[SendMessage]] = {
    val buttons = Seq(
      Seq(
        InlineKeyboardButton("Розірвати шлюб", Some("marriage")),
        InlineKeyboardButton("Стягнути борг", Some("dept"))
      )
    )
    val keyboard = InlineKeyboardMarkup(buttons)

    Future successful {
      Some(
        SendMessage(Left(chatId),
          """
            |Я допоможу Тобі підготувати необхідні для суду документи без юриста.
            |Що тобі потрібно?
          """.stripMargin,
          replyMarkup = Some(keyboard))
      )
    }
  }

  def register_marriage(chatId: Long): Future[Option[SendMessage]] = {
    val buttons = Seq(
      Seq(
        InlineKeyboardButton("ТАК, БЕЗ ПИТАНЬ", Some("doc:scan")),
        InlineKeyboardButton("КРАЩЕ ФОТО З ТЕЛЕФОНУ", Some("doc:photo"))
      )
    )
    val keyboard = InlineKeyboardMarkup(buttons)

    Future successful {
      Some(
        SendMessage(Left(chatId),
          """
            |МЕНІ ПОТРІБНІ ВІД ТЕБЕ ДЕЯКІ ДОКУМЕНТИ.
            |НЕ ХВИЛЮЙСЯ, ВСЕ КОНФІДЕНЦІЙНО.
            |
            |Я СПРОБУЮ САМ ВІДНАЙТИ В ДОКУМЕНТАХ НЕОБХІДНУ ІНФОРМАЦІЮ.
            |ЯКЩО НЕ ЗМОЖУ, ПОПРОШУ ТЕБЕ ВВЕСТИ ВРУЧНУ.
            |
            |ТОБІ ЗРУЧНО ДАТИ МЕНІ СКАНИ?
            |
          """.stripMargin,
          replyMarkup = Some(keyboard))
      )
    }
  }

  def ask(chatId: Long, text: String, redisKey: String): Future[Option[SendMessage]] = {
    sendMessageToChat(SendMessage(Left(chatId), text,
      replyMarkup = Some(ForceReply()))).map { mid =>
      cache.set(s"reply:$chatId:$mid", redisKey)
      None
    }
  }

  def process_scan(chatId: Long, msg: Message): Future[Option[SendMessage]] = {
    recognitionHelper.recoginzeScan().map { rr =>
      cache.set(s"scan:$chatId", rr)
      sendMessageToChat(
        SendMessage(
          Left(chatId), {
            val lst = List(
              rr.nameHusband.map(x => s"Ім'я чоловіка: $x"),
              rr.nameWife.map(x => s"Ім'я дружини: $x"),
              rr.date.map(x => s"Дата реєстарції: $x"),
              rr.actRecordNumber.map(x => s"Номер акту рестрації: $x"),
              rr.series.map(x => s"Серія: $x"),
              rr.number.map(x => s"Номер: $x")
            )
            if (lst.nonEmpty)
              "Я зміг розпізнати такі дані" + lst.mkString("\n")
            else "Я не зміг розпізнати ніяких даних"
          }
        )
      )
    }

    Future successful Some(SendMessage(Left(chatId), "СУПЕР! ЗАРАЗ СПРОБУЮ ВЗЯТИ ПОТРІБНУ ІНФУ"))

  }

  def process_photo(chatId: Long, msg: Message): Future[Option[SendMessage]] =

    Future successful Some(SendMessage(Left(chatId), "СУПЕР! ЗАРАЗ СПРОБУЮ ВЗЯТИ ПОТРІБНУ ІНФУ"))

  def process_reply(msg: Message, replyTo: Message, mode: Option[Int]): Future[Option[SendMessage]] = {

    val cv = cache.get[String](s"reply:${msg.chat.id}:${replyTo.messageId}")
    cv.map(_.split(":").toList) map {
      case "scan" :: Nil if mode.isDefined =>
        process_scan(msg.chat.id, msg)

      case "photo" :: Nil if mode.isDefined =>
        process_photo(msg.chat.id, msg)


    } getOrElse (Future successful errorMsg(msg.chat.id))
  }

  def redirectMessageToChat(msg: ForwardMessage): Future[Int] = {
    ws.url(url + "/forwardMessage")
      .post(Json.parse(toJson(msg).toString)).map {
      x => (x.json \ "result" \ "message_id").as[Int]
    }
  }

  def sendMessageToChat(sendMsg: SendMessage): Future[Int] = {
    ws.url(url + "/sendMessage")
      .post(Json.parse(toJson(sendMsg).toString)).map {
      x => (x.json \ "result" \ "message_id").as[Int]
    }
  }

  def sendPictureToChat(sendMsg: SendPhoto): Future[Int] = {
    println(Json.parse(toJson(sendMsg).toString))
    ws.url(url + "/sendPhoto")
      .post(Json.parse(toJson(sendMsg).toString)).map {
      x => (x.json \ "result" \ "message_id").as[Int]
    }
  }

  def editMessageInChat(editMsg: EditMessageText): Future[Int] = {
    ws.url(url + "/editMessageText")
      .post(Json.parse(toJson(editMsg).toString)).map {
      x => (x.json \ "result" \ "message_id").as[Int]
    }
  }

  def errorMsg(chatId: Long): Option[SendMessage] = {
    Some(SendMessage(Left(chatId),
      """
        |Возникла проблема.
        |Пожалуйста, напишите об этом в поддержку, (комманда /feedback ) указав имя и номер телефона.
      """.stripMargin
    )
    )
  }

  def getCommand(msg: Message): Option[String] = {
    msg.entities.getOrElse(Seq()).find {
      me =>
        me.`type` == "bot_command" && me.offset == 0
    }.flatMap {
      me =>
        msg.text.map(_.slice(0, me.length))
    }
  }

  def setWebhook(): Future[WSResponse] = {
    Thread.sleep(1000)
    val bodyParts = List(
      new StringPart("url", "https://52.232.84.122/inbox", "UTF-8"),
      new FilePart("certificate", new File(s"${
        conf.getString("filePrefix").get
      }public/certificates/server.pem"))
    )
    val client = ws.underlying.asInstanceOf[AsyncHttpClient]

    val builder = client.preparePost(url + "/setWebhook")

    builder.setHeader("Content-Type", "multipart/form-data")
    bodyParts.foreach(builder.addBodyPart)

    val result = Promise[WSResponse]()

    client.executeRequest(builder.build(), new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response): Response = {
        result.success(AhcWSResponse(response))
        response
      }

      override def onThrowable(t: Throwable): Unit = {
        result.failure(t)
      }
    })

    result.future
  }

  def toJson[T](t: T): String = compact(render4s(Extraction.decompose(t).underscoreKeys))

  def toAnswerJson[T](t: T, method: String): JsValue = Json.parse(
    compact(
      render4s(new JObject(Extraction.decompose(t).asInstanceOf[JObject].obj ++
        List("method" -> JString(method))).underscoreKeys)
    )
  )

  def fromJson[T: Manifest](json: String): T = parse4s(json).camelizeKeys.extract[T]

  def genModel(pass: Option[String]) = Action {
    val res = for {
      p <- pass if p == "qwerty"
    } yield {
      slick.codegen.SourceCodeGenerator.main(
        Array(
          "slick.driver.PostgresDriver",
          "org.postgresql.Driver",
          "jdbc:postgresql://52.232.84.122:5432/tradehubbot",
          "app",
          "models",
          "bot",
          "root")
      )
      Ok("success gen")
    }
    res.getOrElse(BadRequest("access denied"))
  }
}
