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
import telegram._
import telegram.methods._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success, Try}

class ApplicationController @Inject()(ws: WSClient, conf: play.api.Configuration, cache: CacheApi)
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
          case _ =>
            msg.replyToMessage match {
              case Some(x) if msg.text.isDefined =>
                process_reply(msg, x, mode)
              case _ =>
                Future successful SendMessage(Left(msg.chat.id), "Я не понимаю этой комманды")

            }
        }

      case (None, Some(cbq)) =>
        val callbackData = cbq.data.map(_.split(";")) flatMap {
          case Array(c, v) => Some((c, v))
          case _ => None
        }
        callbackData match {
//          case Some((cbCom, cbVal)) =>
//            cbCom match {
//              case "ms" => monitoring_stop(cbq.from.id, cbVal)
//            }
          case _ => Future successful errorMsg(cbq.from.id)

        }
      case x =>
        println("Unknown update " + update)
        Future successful SendMessage(Right(""), "")
    }
    response.map { x =>
      if (x.chatId.isLeft) {
        Ok(toAnswerJson(x, x.methodName))
      } else Ok
    }
  }

//  def showAnalitycs(chatId: Long): Future[SendMessage] = {
//    sendMessageToChat(SendMessage(
//      Left(chatId),
//      """
//        |Прогноз для товара "Сахар"
//        |
//        |Цена на сегодня: 15037 грн/т
//        |
//        |на 1  неделю: 15196 грн/т _+159  грн/т (+ 1.06  %)_
//        |на 5  недель: 16202 грн/т _+1165 грн/т (+ 6.65  %)_
//        |на 10 недель: 17012 грн/т _+1975 грн/т (+ 13.13 %)_
//        |
//        |Изменение цены с доверительным интервалом в 90%:
//      """.stripMargin,
//      parseMode = Some(ParseMode.Markdown)
//    )).onComplete { _ =>
//      val bodyParts = List(
//        new StringPart("chat_id", s"$chatId", "UTF-8"),
//        new FilePart("photo", new File(s"${
//          conf.getString("filePrefix").get
//        }public/images/sugar90new.png"))
//      )
//      val client = ws.underlying.asInstanceOf[AsyncHttpClient]
//
//      val builder = client.preparePost(url + "/sendPhoto")
//
//      builder.setHeader("Content-Type", "multipart/form-data")
//      bodyParts.foreach(builder.addBodyPart)
//
//      val result = Promise[WSResponse]()
//
//      client.executeRequest(builder.build(), new AsyncCompletionHandler[Response]() {
//        override def onCompleted(response: Response): Response = {
//          result.success(AhcWSResponse(response))
//          response
//        }
//
//        override def onThrowable(t: Throwable): Unit = {
//          result.failure(t)
//        }
//      })
//    }
//    Future successful SendMessage(Right(""), "")
//  }
//
//  def askQuantity(chatId: Long, bidId: Int): Future[SendMessage] = {
//    sendMessageToChat(SendMessage(Left(chatId), "Введите количество едениц товара", replyMarkup = Some(ForceReply()))).map { mid =>
//      cache.set(s"reply:$chatId:$mid", "quantity")
//      SendMessage(Left(chatId), "Например просто \"500\" или \"0.2\"")
//    }
//  }
//
//  def askPrice(chatId: Long): Future[SendMessage] = {
//    sendMessageToChat(SendMessage(Left(chatId),
//      s"""Укажите цену за единицу в гривнах.""".stripMargin,
//      replyMarkup = Some(ForceReply()))).map { mid =>
//      cache.set(s"reply:$chatId:$mid", "price")
//      SendMessage(Left(chatId), "Например просто \"12543.2\" или \"10\"")
//    }
//  }
//
//  def askTax(chatId: Long, bidId: Int): Future[SendMessage] = {
//
//    val keyboard = InlineKeyboardMarkup(
//      Seq(
//        Seq(InlineKeyboardButton("Да", Some(s"c_b6;y:$bidId"))),
//        Seq(InlineKeyboardButton("Нет", Some(s"c_b6;n:$bidId")))
//      )
//    )
//    Future successful SendMessage(Left(chatId), "Поставка будет с НДС?", replyMarkup = Some(keyboard))
//  }
//
//  def askDate(chatId: Long, redisKey: String): Future[SendMessage] = {
//    sendMessageToChat(SendMessage(Left(chatId),
//      s"""Укажите дату в формате ДД.ММ.ГГГГ .""".stripMargin,
//      replyMarkup = Some(ForceReply()))).map { mid =>
//      cache.set(s"reply:$chatId:$mid", redisKey)
//      SendMessage(Left(chatId), "Например \"01.04.2017\" или \"27.06.2018\"")
//    }
//  }

  def start(chatId: Long): Future[SendMessage] = {
    val buttons = Seq(
      Seq(
        KeyboardButton("Розірвати шлюб"),
        KeyboardButton("Стягнути борг")
      )
    )
    val keyboard = ReplyKeyboardMarkup(buttons, oneTimeKeyboard = Some(true))

    Future successful {
      SendMessage(Left(chatId),
        """
          |ПРИВІТ! Я СУДОБОТ.
          |Я допоможу Тобі підготувати необхідні для суду документи без юриста.
          |Що тобі потрібно?
        """.stripMargin,
        replyMarkup = Some(keyboard))
    }
  }

  def process_reply(msg: Message, replyTo: Message, mode: Option[Int]): Future[SendMessage] = {

    val cv = cache.get[String](s"reply:${msg.chat.id}:${replyTo.messageId}")
    cv.map(_.split(":").toList) map {
      case "auth_code" :: Nil if mode.isDefined =>
        Future successful errorMsg(msg.chat.id)
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

  def errorMsg(chatId: Long): SendMessage = {
    SendMessage(Left(chatId),
      """
        |Возникла проблема.
        |Пожалуйста, напишите об этом в поддержку, (комманда /feedback ) указав имя и номер телефона.
      """.stripMargin
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
