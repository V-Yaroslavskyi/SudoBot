package processors

import java.util.Date
import javax.inject.Inject

import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by v-yaroslavskyi on 4/22/17.
  */

case class RecognitionResult(nameHusband: Option[String], nameWife: Option[String], date:Option[Date], series: Option[String], number: Option[BigDecimal], actRecordNumber: Option[BigDecimal])

class RecognitionHelper @Inject()(ws: WSClient)(implicit val exc: ExecutionContext){

  def recoginzeScan() = Future successful RecognitionResult(None, None, None, None, None, None)
  def recoginzePhoto() = Future successful RecognitionResult(None, None, None, None, None, None)
}
