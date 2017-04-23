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
import processors.{RecognitionHelper, RecognitionResult}
import telegram._
import telegram.methods._
import utilities.{Child, PoiReplacer}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success, Try}

class ApplicationController @Inject()(ws: WSClient, conf: play.api.Configuration,
                                      cache: CacheApi, recognitionHelper: RecognitionHelper,
                                     poiReplacer: PoiReplacer)
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
//    val reps =  Map(
//      "@courtName" -> "BEST COURT EVER", "@courtAddr" -> "BEST COURT ADDR",
//      "@plaintiffName" -> "Іваненко Ганна Дмитрівна", "@plaintiffAddr" -> "addr 1", "@plaintiffMailAddr" -> "mail addr 1", "@plaintiffTaxId" -> "1111111111111111111",
//      "@defendantName" -> "Петренко Джон Людвігович", "@defendantAddr" -> "def addr", "@defendantRealAddr" -> "def real addr", "@defendantTaxId" -> "222222222222222222",
//      "@childrenLiveWith" -> "з матір'ю",
//      "@marriageDate" -> "01.01.1991", "@registeredBy" -> " ZAGS", "@actNumber" -> "777", "@actSeries" -> "TT", "@actNumber" -> "123456", "@marriageSertDate" -> "02.01.1991"
//    )
//
//    val cont = poiReplacer.buildDoc(reps, List(new Child("AA B. C.", "3.4.2009", "8"),new Child("AA B. C.", "1.2.2008", "9") ), "NAME_OF_RES")
//    Ok.sendFile(cont)
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
              case "suit" =>
                cbVal match {
                  case "marriage" =>
                    register_marriage(cbq.from.id)
                  case "debt" =>
                    Future successful Some(SendMessage(Left(cbq.from.id), "Ця функція на даний момент недоступна"))
                }


              case "doc" =>
                cbVal match {
                  case "scan" => ask(cbq.from.id,
                    s"""
                       |ОК. ТОДІ ЗІСКАНУЙ МЕНІ, БУДЬ ЛАСКА, КОПІЮ СВОГО СВІДОЦТВА ПРО ШЛЮБ
                       |Ще трішки і цей документ перестане діяти :)
                    """.stripMargin, "scan")

                  case "photo" => ask(cbq.from.id, "ОК, ТОДІ СФОТКАЙ, БУДЬ ЛАСКА, СВОЄ СВІДОЦТВО ПРО ШЛЮБ", "scan")

                  case "none" =>
                    cache.set(s"scan:${cbq.from.id}", RecognitionResult(None, None, None, None, None, None))
                    askNextQuestion(cbq.from.id)
                }

              case "change" =>
                ask(cbq.from.id, "Введіть нове значення", cbVal)

              case "status" =>
                askChildren(cbq.from.id)

              case "children" =>
                cbVal match {
                  case "yes" =>
                    ask(cbq.from.id, "Введіть повне ім'я дитини", "childName")

                  case "no" =>
                    ask(cbq.from.id,
                      s"""
                         |ОК. ТОДІ ЗІСКАНУЙ МЕНІ, БУДЬ ЛАСКА, КОПІЮ СВОГО СВІДОЦТВА ПРО ШЛЮБ
                         |Ще трішки і цей документ перестане діяти :)
                    """.stripMargin, "scan")
                }

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

  def askChildren(chatId: Long, firstTime: Boolean = true): Future[Option[SendMessage]] = {
    val buttons = Seq(
      Seq(InlineKeyboardButton("ТАК", Some("children;yes"))),
      Seq(InlineKeyboardButton("НІ", Some("children;no")))
    )

    val keyboard = InlineKeyboardMarkup(buttons)
    val cld = cache.get[List[(String, Option[Date])]](s"children:$chatId").getOrElse(List())
    Future successful {
      Some(
        SendMessage(Left(chatId), if(firstTime) "У вас є діти?" else cld.map(x => s"${x._1} (${x._2})") + "У вас є ще діти?", replyMarkup = Some(keyboard))
      )
    }
  }

  def create_suit(chatId: Long): Future[Option[SendMessage]] = {
    val buttons = Seq(
      Seq(
        InlineKeyboardButton("Розірвати шлюб", Some("suit;marriage")),
        InlineKeyboardButton("Стягнути борг", Some("suit;debt"))
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
      Seq(InlineKeyboardButton("ТАК, БЕЗ ПИТАНЬ", Some("doc;scan"))),
      Seq(InlineKeyboardButton("КРАЩЕ ФОТО З ТЕЛЕФОНУ", Some("doc;photo"))),
      Seq(InlineKeyboardButton("В МЕНЕ ЙОГО НАРАЗІ НЕМАЄ", Some("doc;none")))

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

  def askNextQuestion(chatId: Long): Future[Option[SendMessage]] = {
    cache.get[RecognitionResult](s"scan:$chatId").map {
      case RecognitionResult(None, _, _, _, _, _) => ask(chatId, "Введіть і'мя чоловіка", "husbandName")
      case RecognitionResult(Some(_), None, _, _, _, _) => ask(chatId, "Введіть і'мя дружини", "wifeName")
      case RecognitionResult(Some(_), Some(_), None, _, _, _) => ask(chatId, "Введіть дату одруження", "date")
      case RecognitionResult(Some(_), Some(_), Some(_), None, _, _) => ask(chatId, "Введіть серію свідотства", "series")
      case RecognitionResult(Some(_), Some(_), Some(_), Some(_), None, _) => ask(chatId, "Введіть номер свідотства", "number")
      case RecognitionResult(Some(_), Some(_), Some(_), Some(_), Some(_), None) => ask(chatId, "Введіть номер акту реєстрації", "actRegistrationNumber")
      case RecognitionResult(Some(_), Some(_), Some(_), Some(_), Some(_), Some(_)) => check_data(chatId)
    }.getOrElse(
      Future successful errorMsg(chatId)
    )
  }


  def check_data(chatId: Long): Future[Option[SendMessage]] = {
    Future successful
      cache.get[RecognitionResult](s"scan:$chatId").flatMap { rr =>
        val lst = List(
          rr.nameHusband.map(x => s"Ім'я чоловіка: $x"),
          rr.nameWife.map(x => s"Ім'я дружини: $x"),
          rr.date.map(x => s"Дата реєстарції: ${dtf.print(x.getTime)}"),
          rr.actRecordNumber.map(x => s"Номер акту рестрації: $x"),
          rr.series.map(x => s"Серія свідотсва про шлюб: $x"),
          rr.number.map(x => s"Номер свідотства про шлюб: $x")
        )
        val buttons = Seq(
          Seq(
            InlineKeyboardButton("Ім'я чоловіка", Some("change;husbandName")),
            InlineKeyboardButton("Ім'я дружини", Some("change;wifeName"))
          ),
          Seq(
            InlineKeyboardButton("Дата реєстарції", Some("change;date")),
            InlineKeyboardButton("Номер акту рестрації", Some("change;actRegistrationNumber"))
          ),
          Seq(
            InlineKeyboardButton("Серія свідотсва про шлюб", Some("change;series")),
            InlineKeyboardButton("Номер свідотства про шлюб", Some("change;number"))
          ),
          Seq(
            InlineKeyboardButton("Все вірно", Some("status;ok"))
          )
        )
        val keyboard = InlineKeyboardMarkup(buttons)

        Some(
          SendMessage(
            Left(chatId),
            "Введені дані:" + lst.flatten.mkString("\n", "\n", "\n") + " Якщо хочеш щось змінити - натисни відповідну кнопку знизу.",
            replyMarkup = Some(keyboard)
          )
        )
      }
  }

  def process_scan(chatId: Long, msg: Message): Future[Option[SendMessage]] = {
    recognitionHelper.recoginzeScan().map { rr =>
      cache.set(s"scan:$chatId", rr)
      val lst = List(
        rr.nameHusband.map(x => s"Ім'я чоловіка: $x"),
        rr.nameWife.map(x => s"Ім'я дружини: $x"),
        rr.date.map(x => s"Дата реєстарції: ${dtf.print(x.getTime)}"),
        rr.actRecordNumber.map(x => s"Номер акту рестрації: $x"),
        rr.series.map(x => s"Серія свідотсва про шлюб: $x"),
        rr.number.map(x => s"Номер свідотства про шлюб: $x")
      )
      sendMessageToChat(
        SendMessage(
          Left(chatId), {
            if (lst.flatten.nonEmpty)
              "Я зміг розпізнати такі дані:\n" + lst.flatten.mkString("\n")
            else "Я не зміг розпізнати ніяких даних"
          }
        )
      )
      askNextQuestion(chatId)
    }

    Future successful Some(SendMessage(Left(chatId), "СУПЕР! ЗАРАЗ СПРОБУЮ ВЗЯТИ ПОТРІБНУ ІНФУ"))

  }

  def process_photo(chatId: Long, msg: Message): Future[Option[SendMessage]] =

    Future successful Some(SendMessage(Left(chatId), "СУПЕР! ЗАРАЗ СПРОБУЮ ВЗЯТИ ПОТРІБНУ ІНФУ"))

  def process_reply(msg: Message, replyTo: Message, mode: Option[Int]): Future[Option[SendMessage]] = {

    val cv = cache.get[String](s"reply:${msg.chat.id}:${replyTo.messageId}")
    cv.map(_.split(":").toList) map {
      case "scan" :: Nil =>
        process_scan(msg.chat.id, msg)

      case "photo" :: Nil =>
        process_photo(msg.chat.id, msg)

      case "husbandName" :: Nil =>
        cache.get[RecognitionResult](s"scan:${msg.chat.id}").map { x =>
          cache.set(s"scan:${msg.chat.id}", x.copy(nameHusband = msg.text))
          sendMessageToChat(
            msg.text.map { r =>
              SendMessage(Left(msg.chat.id), s"Повне ім'я чоловіка вказано як $r")
            }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
          )
        }
        askNextQuestion(msg.chat.id)

      case "wifeName" :: Nil =>
        cache.get[RecognitionResult](s"scan:${msg.chat.id}").map { x =>
          cache.set(s"scan:${msg.chat.id}", x.copy(nameWife = msg.text))
          sendMessageToChat(
            msg.text.map { r =>
              SendMessage(Left(msg.chat.id), s"Повне ім'я дружини вказано як $r")
            }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
          )
        }
        askNextQuestion(msg.chat.id)

      case "date" :: Nil =>
        cache.get[RecognitionResult](s"scan:${msg.chat.id}").map { x =>
          val v = msg.text.flatMap(t => Try(dtf.parseLocalDate(t).toDate).toOption)
          cache.set(s"scan:${msg.chat.id}", x.copy(date = v))
          sendMessageToChat(
            v.map { r =>
              SendMessage(Left(msg.chat.id), s"Дата одруження вказана як ${dtf.print(r.getTime)}")
            }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
          )
        }
        askNextQuestion(msg.chat.id)

      case "series" :: Nil =>
        cache.get[RecognitionResult](s"scan:${msg.chat.id}").map { x =>
          cache.set(s"scan:${msg.chat.id}", x.copy(series = msg.text))
          sendMessageToChat(
            msg.text.map { r =>
              SendMessage(Left(msg.chat.id), s"Серія свідотсва про шлюб вказана як $r")
            }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
          )
        }
        askNextQuestion(msg.chat.id)

      case "number" :: Nil =>
        cache.get[RecognitionResult](s"scan:${msg.chat.id}").map { x =>
          val v = msg.text.flatMap(t => Try(BigDecimal(t)).toOption)
          cache.set(s"scan:${msg.chat.id}", x.copy(number = v))
          sendMessageToChat(
            v.map { r =>
              SendMessage(Left(msg.chat.id), s"Номер свідотсва про шлюб вказаний як $r")
            }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
          )
        }
        askNextQuestion(msg.chat.id)

      case "actRegistrationNumber" :: Nil =>
        cache.get[RecognitionResult](s"scan:${msg.chat.id}").map { x =>
          val v = msg.text.flatMap(t => Try(BigDecimal(t)).toOption)
          cache.set(s"scan:${msg.chat.id}", x.copy(actRecordNumber = v))
          sendMessageToChat(
            v.map { r =>
              SendMessage(Left(msg.chat.id), s"Номер акту реєстрації вказано як $r")
            }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
          )
        }
        askNextQuestion(msg.chat.id)

      case "childName" :: Nil =>
        sendMessageToChat(
          msg.text.map { r =>
            SendMessage(Left(msg.chat.id), s"Повне ім'я дитини вказано як $r")
          }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
        )
        msg.text.map {x =>
          val cld = cache.get[List[(String, Option[Date])]](s"children:${msg.chat.id}").getOrElse(List())
          cache.set(s"children:${msg.chat.id}", (x, None) :: cld)
          ask(msg.chat.id, "Введіть дату народження дитини", "childDate")
        }.getOrElse(ask(msg.chat.id, "Введіть повне ім'я дитини", "childName"))

      case "childDate" :: Nil =>
        val v = msg.text.flatMap(t => Try(dtf.parseLocalDate(t).toDate).toOption)
        sendMessageToChat(
        v.map { r =>
            SendMessage(Left(msg.chat.id), s"Дата народження дитини вказано як $r")
          }.getOrElse(SendMessage(Left(msg.chat.id), "Помилка розпізнавання даних"))
        )
        msg.text.map {x =>
          val cld = cache.get[List[(String, Option[Date])]](s"children:${msg.chat.id}").getOrElse(List())
          cache.set(s"children:${msg.chat.id}", (cld.head, v) :: cld.tail)
          askChildren(msg.chat.id)
        }.getOrElse(ask(msg.chat.id, "Введіть дату народження дитини", "childDate"))


      case _ =>
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
