package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.driver.PostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(ApiUsers.schema, Bids.schema, Commodities.schema, Companies.schema, Contracts.schema, MonitoringNews.schema, Tenders.schema, TgAddresses.schema, UsersChats.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table ApiUsers
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param login Database column login SqlType(varchar), Length(255,true)
   *  @param password Database column password SqlType(varchar), Length(255,true), Default(None)
   *  @param name Database column name SqlType(varchar), Length(255,true) */
  case class ApiUsersRow(id: Int, login: String, password: Option[String] = None, name: String)
  /** GetResult implicit for fetching ApiUsersRow objects using plain SQL queries */
  implicit def GetResultApiUsersRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[ApiUsersRow] = GR{
    prs => import prs._
    ApiUsersRow.tupled((<<[Int], <<[String], <<?[String], <<[String]))
  }
  /** Table description of table api_users. Objects of this class serve as prototypes for rows in queries. */
  class ApiUsers(_tableTag: Tag) extends Table[ApiUsersRow](_tableTag, "api_users") {
    def * = (id, login, password, name) <> (ApiUsersRow.tupled, ApiUsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(login), password, Rep.Some(name)).shaped.<>({r=>import r._; _1.map(_=> ApiUsersRow.tupled((_1.get, _2.get, _3, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column login SqlType(varchar), Length(255,true) */
    val login: Rep[String] = column[String]("login", O.Length(255,varying=true))
    /** Database column password SqlType(varchar), Length(255,true), Default(None) */
    val password: Rep[Option[String]] = column[Option[String]]("password", O.Length(255,varying=true), O.Default(None))
    /** Database column name SqlType(varchar), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table ApiUsers */
  lazy val ApiUsers = new TableQuery(tag => new ApiUsers(tag))

  /** Entity class storing rows of table Bids
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param authorId Database column author_id SqlType(int4)
   *  @param note Database column note SqlType(text)
   *  @param contractId Database column contract_id SqlType(int4)
   *  @param consumerConfirmed Database column consumer_confirmed SqlType(bool), Default(None)
   *  @param producerConfirmed Database column producer_confirmed SqlType(bool), Default(None) */
  case class BidsRow(id: Int, authorId: Int, note: String, contractId: Int, consumerConfirmed: Option[Boolean] = None, producerConfirmed: Option[Boolean] = None)
  /** GetResult implicit for fetching BidsRow objects using plain SQL queries */
  implicit def GetResultBidsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[Boolean]]): GR[BidsRow] = GR{
    prs => import prs._
    BidsRow.tupled((<<[Int], <<[Int], <<[String], <<[Int], <<?[Boolean], <<?[Boolean]))
  }
  /** Table description of table bids. Objects of this class serve as prototypes for rows in queries. */
  class Bids(_tableTag: Tag) extends Table[BidsRow](_tableTag, "bids") {
    def * = (id, authorId, note, contractId, consumerConfirmed, producerConfirmed) <> (BidsRow.tupled, BidsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(authorId), Rep.Some(note), Rep.Some(contractId), consumerConfirmed, producerConfirmed).shaped.<>({r=>import r._; _1.map(_=> BidsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column author_id SqlType(int4) */
    val authorId: Rep[Int] = column[Int]("author_id")
    /** Database column note SqlType(text) */
    val note: Rep[String] = column[String]("note")
    /** Database column contract_id SqlType(int4) */
    val contractId: Rep[Int] = column[Int]("contract_id")
    /** Database column consumer_confirmed SqlType(bool), Default(None) */
    val consumerConfirmed: Rep[Option[Boolean]] = column[Option[Boolean]]("consumer_confirmed", O.Default(None))
    /** Database column producer_confirmed SqlType(bool), Default(None) */
    val producerConfirmed: Rep[Option[Boolean]] = column[Option[Boolean]]("producer_confirmed", O.Default(None))

    /** Foreign key referencing Companies (database name bids_companies_company_id_fk) */
    lazy val companiesFk = foreignKey("bids_companies_company_id_fk", authorId, Companies)(r => r.companyId, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Bids */
  lazy val Bids = new TableQuery(tag => new Bids(tag))

  /** Entity class storing rows of table Commodities
   *  @param commodityId Database column commodity_id SqlType(serial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Length(256,true)
   *  @param measurementUnits Database column measurement_units SqlType(varchar), Length(255,true)
   *  @param code Database column code SqlType(varchar), Length(255,true) */
  case class CommoditiesRow(commodityId: Int, name: String, measurementUnits: String, code: String)
  /** GetResult implicit for fetching CommoditiesRow objects using plain SQL queries */
  implicit def GetResultCommoditiesRow(implicit e0: GR[Int], e1: GR[String]): GR[CommoditiesRow] = GR{
    prs => import prs._
    CommoditiesRow.tupled((<<[Int], <<[String], <<[String], <<[String]))
  }
  /** Table description of table commodities. Objects of this class serve as prototypes for rows in queries. */
  class Commodities(_tableTag: Tag) extends Table[CommoditiesRow](_tableTag, "commodities") {
    def * = (commodityId, name, measurementUnits, code) <> (CommoditiesRow.tupled, CommoditiesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(commodityId), Rep.Some(name), Rep.Some(measurementUnits), Rep.Some(code)).shaped.<>({r=>import r._; _1.map(_=> CommoditiesRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column commodity_id SqlType(serial), AutoInc, PrimaryKey */
    val commodityId: Rep[Int] = column[Int]("commodity_id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Length(256,true) */
    val name: Rep[String] = column[String]("name", O.Length(256,varying=true))
    /** Database column measurement_units SqlType(varchar), Length(255,true) */
    val measurementUnits: Rep[String] = column[String]("measurement_units", O.Length(255,varying=true))
    /** Database column code SqlType(varchar), Length(255,true) */
    val code: Rep[String] = column[String]("code", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Commodities */
  lazy val Commodities = new TableQuery(tag => new Commodities(tag))

  /** Entity class storing rows of table Companies
   *  @param companyId Database column company_id SqlType(serial), AutoInc, PrimaryKey
   *  @param regNumber Database column reg_number SqlType(varchar), Length(50,true)
   *  @param signerName Database column signer_name SqlType(varchar), Length(256,true)
   *  @param bankAccount Database column bank_account SqlType(varchar), Length(20,true)
   *  @param `e-mail` Database column e-mail SqlType(varchar), Length(256,true)
   *  @param telephone Database column telephone SqlType(varchar), Length(50,true)
   *  @param companyName Database column company_name SqlType(varchar), Length(255,true)
   *  @param signerDocDate Database column signer_doc_date SqlType(date), Default(None)
   *  @param signerDocNumber Database column signer_doc_number SqlType(varchar), Length(255,true), Default(None)
   *  @param regInstitution Database column reg_institution SqlType(varchar), Length(255,true)
   *  @param bankName Database column bank_name SqlType(varchar), Length(255,true) */
  case class CompaniesRow(companyId: Int, regNumber: String, signerName: String, bankAccount: String, `e-mail`: String, telephone: String, companyName: String, signerDocDate: Option[java.sql.Date] = None, signerDocNumber: Option[String] = None, regInstitution: String, bankName: String)
  /** GetResult implicit for fetching CompaniesRow objects using plain SQL queries */
  implicit def GetResultCompaniesRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]]): GR[CompaniesRow] = GR{
    prs => import prs._
    CompaniesRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<?[java.sql.Date], <<?[String], <<[String], <<[String]))
  }
  /** Table description of table companies. Objects of this class serve as prototypes for rows in queries. */
  class Companies(_tableTag: Tag) extends Table[CompaniesRow](_tableTag, "companies") {
    def * = (companyId, regNumber, signerName, bankAccount, `e-mail`, telephone, companyName, signerDocDate, signerDocNumber, regInstitution, bankName) <> (CompaniesRow.tupled, CompaniesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(companyId), Rep.Some(regNumber), Rep.Some(signerName), Rep.Some(bankAccount), Rep.Some(`e-mail`), Rep.Some(telephone), Rep.Some(companyName), signerDocDate, signerDocNumber, Rep.Some(regInstitution), Rep.Some(bankName)).shaped.<>({r=>import r._; _1.map(_=> CompaniesRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8, _9, _10.get, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column company_id SqlType(serial), AutoInc, PrimaryKey */
    val companyId: Rep[Int] = column[Int]("company_id", O.AutoInc, O.PrimaryKey)
    /** Database column reg_number SqlType(varchar), Length(50,true) */
    val regNumber: Rep[String] = column[String]("reg_number", O.Length(50,varying=true))
    /** Database column signer_name SqlType(varchar), Length(256,true) */
    val signerName: Rep[String] = column[String]("signer_name", O.Length(256,varying=true))
    /** Database column bank_account SqlType(varchar), Length(20,true) */
    val bankAccount: Rep[String] = column[String]("bank_account", O.Length(20,varying=true))
    /** Database column e-mail SqlType(varchar), Length(256,true) */
    val `e-mail`: Rep[String] = column[String]("e-mail", O.Length(256,varying=true))
    /** Database column telephone SqlType(varchar), Length(50,true) */
    val telephone: Rep[String] = column[String]("telephone", O.Length(50,varying=true))
    /** Database column company_name SqlType(varchar), Length(255,true) */
    val companyName: Rep[String] = column[String]("company_name", O.Length(255,varying=true))
    /** Database column signer_doc_date SqlType(date), Default(None) */
    val signerDocDate: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("signer_doc_date", O.Default(None))
    /** Database column signer_doc_number SqlType(varchar), Length(255,true), Default(None) */
    val signerDocNumber: Rep[Option[String]] = column[Option[String]]("signer_doc_number", O.Length(255,varying=true), O.Default(None))
    /** Database column reg_institution SqlType(varchar), Length(255,true) */
    val regInstitution: Rep[String] = column[String]("reg_institution", O.Length(255,varying=true))
    /** Database column bank_name SqlType(varchar), Length(255,true) */
    val bankName: Rep[String] = column[String]("bank_name", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Companies */
  lazy val Companies = new TableQuery(tag => new Companies(tag))

  /** Entity class storing rows of table Contracts
   *  @param contractId Database column contract_id SqlType(serial), AutoInc, PrimaryKey
   *  @param contractNumber Database column contract_number SqlType(varchar), Length(255,true)
   *  @param commodityId Database column commodity_id SqlType(int4)
   *  @param consumerId Database column consumer_id SqlType(int4)
   *  @param producerId Database column producer_id SqlType(int4)
   *  @param contractDate Database column contract_date SqlType(date) */
  case class ContractsRow(contractId: Int, contractNumber: String, commodityId: Int, consumerId: Int, producerId: Int, contractDate: java.sql.Date)
  /** GetResult implicit for fetching ContractsRow objects using plain SQL queries */
  implicit def GetResultContractsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.sql.Date]): GR[ContractsRow] = GR{
    prs => import prs._
    ContractsRow.tupled((<<[Int], <<[String], <<[Int], <<[Int], <<[Int], <<[java.sql.Date]))
  }
  /** Table description of table contracts. Objects of this class serve as prototypes for rows in queries. */
  class Contracts(_tableTag: Tag) extends Table[ContractsRow](_tableTag, "contracts") {
    def * = (contractId, contractNumber, commodityId, consumerId, producerId, contractDate) <> (ContractsRow.tupled, ContractsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(contractId), Rep.Some(contractNumber), Rep.Some(commodityId), Rep.Some(consumerId), Rep.Some(producerId), Rep.Some(contractDate)).shaped.<>({r=>import r._; _1.map(_=> ContractsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column contract_id SqlType(serial), AutoInc, PrimaryKey */
    val contractId: Rep[Int] = column[Int]("contract_id", O.AutoInc, O.PrimaryKey)
    /** Database column contract_number SqlType(varchar), Length(255,true) */
    val contractNumber: Rep[String] = column[String]("contract_number", O.Length(255,varying=true))
    /** Database column commodity_id SqlType(int4) */
    val commodityId: Rep[Int] = column[Int]("commodity_id")
    /** Database column consumer_id SqlType(int4) */
    val consumerId: Rep[Int] = column[Int]("consumer_id")
    /** Database column producer_id SqlType(int4) */
    val producerId: Rep[Int] = column[Int]("producer_id")
    /** Database column contract_date SqlType(date) */
    val contractDate: Rep[java.sql.Date] = column[java.sql.Date]("contract_date")

    /** Foreign key referencing Commodities (database name contracts_commodities_commodity_id_fk) */
    lazy val commoditiesFk = foreignKey("contracts_commodities_commodity_id_fk", commodityId, Commodities)(r => r.commodityId, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing Companies (database name contracts_companies_company_id_fk) */
    lazy val companiesFk2 = foreignKey("contracts_companies_company_id_fk", producerId, Companies)(r => r.companyId, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing Companies (database name contracts_companies_company_id_fk_consumer) */
    lazy val companiesFk3 = foreignKey("contracts_companies_company_id_fk_consumer", consumerId, Companies)(r => r.companyId, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Contracts */
  lazy val Contracts = new TableQuery(tag => new Contracts(tag))

  /** Entity class storing rows of table MonitoringNews
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param chatId Database column chat_id SqlType(int4)
   *  @param keywords Database column keywords SqlType(text)
   *  @param active Database column active SqlType(bool) */
  case class MonitoringNewsRow(id: Int, chatId: Int, keywords: String, active: Boolean)
  /** GetResult implicit for fetching MonitoringNewsRow objects using plain SQL queries */
  implicit def GetResultMonitoringNewsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Boolean]): GR[MonitoringNewsRow] = GR{
    prs => import prs._
    MonitoringNewsRow.tupled((<<[Int], <<[Int], <<[String], <<[Boolean]))
  }
  /** Table description of table monitoring_news. Objects of this class serve as prototypes for rows in queries. */
  class MonitoringNews(_tableTag: Tag) extends Table[MonitoringNewsRow](_tableTag, "monitoring_news") {
    def * = (id, chatId, keywords, active) <> (MonitoringNewsRow.tupled, MonitoringNewsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(chatId), Rep.Some(keywords), Rep.Some(active)).shaped.<>({r=>import r._; _1.map(_=> MonitoringNewsRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column chat_id SqlType(int4) */
    val chatId: Rep[Int] = column[Int]("chat_id")
    /** Database column keywords SqlType(text) */
    val keywords: Rep[String] = column[String]("keywords")
    /** Database column active SqlType(bool) */
    val active: Rep[Boolean] = column[Boolean]("active")
  }
  /** Collection-like TableQuery object for table MonitoringNews */
  lazy val MonitoringNews = new TableQuery(tag => new MonitoringNews(tag))

  /** Entity class storing rows of table Tenders
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param startDate Database column start_date SqlType(date)
   *  @param endDate Database column end_date SqlType(date)
   *  @param numberOfBids Database column number_of_bids SqlType(int4)
   *  @param amount Database column amount SqlType(float8)
   *  @param currency Database column currency SqlType(varchar), Length(10,true)
   *  @param taxIncluded Database column tax_included SqlType(bool)
   *  @param title Database column title SqlType(varchar), Length(255,true)
   *  @param description Database column description SqlType(text), Default(None)
   *  @param zpuId Database column zpu_id SqlType(varchar), Length(255,true)
   *  @param link Database column link SqlType(varchar), Length(255,true)
   *  @param lotsText Database column lots_text SqlType(text), Default(None)
   *  @param authorCompany Database column author_company SqlType(varchar), Length(255,true)
   *  @param telephone Database column telephone SqlType(varchar), Length(255,true)
   *  @param status Database column status SqlType(varchar), Length(255,true)
   *  @param isCommercial Database column is_commercial SqlType(bool)
   *  @param stampModified Database column stamp_modified SqlType(int8) */
  case class TendersRow(id: Int, startDate: java.sql.Date, endDate: java.sql.Date, numberOfBids: Int, amount: Double, currency: String, taxIncluded: Boolean, title: String, description: Option[String] = None, zpuId: String, link: String, lotsText: Option[String] = None, authorCompany: String, telephone: String, status: String, isCommercial: Boolean, stampModified: Long)
  /** GetResult implicit for fetching TendersRow objects using plain SQL queries */
  implicit def GetResultTendersRow(implicit e0: GR[Int], e1: GR[java.sql.Date], e2: GR[Double], e3: GR[String], e4: GR[Boolean], e5: GR[Option[String]], e6: GR[Long]): GR[TendersRow] = GR{
    prs => import prs._
    TendersRow.tupled((<<[Int], <<[java.sql.Date], <<[java.sql.Date], <<[Int], <<[Double], <<[String], <<[Boolean], <<[String], <<?[String], <<[String], <<[String], <<?[String], <<[String], <<[String], <<[String], <<[Boolean], <<[Long]))
  }
  /** Table description of table tenders. Objects of this class serve as prototypes for rows in queries. */
  class Tenders(_tableTag: Tag) extends Table[TendersRow](_tableTag, "tenders") {
    def * = (id, startDate, endDate, numberOfBids, amount, currency, taxIncluded, title, description, zpuId, link, lotsText, authorCompany, telephone, status, isCommercial, stampModified) <> (TendersRow.tupled, TendersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(startDate), Rep.Some(endDate), Rep.Some(numberOfBids), Rep.Some(amount), Rep.Some(currency), Rep.Some(taxIncluded), Rep.Some(title), description, Rep.Some(zpuId), Rep.Some(link), lotsText, Rep.Some(authorCompany), Rep.Some(telephone), Rep.Some(status), Rep.Some(isCommercial), Rep.Some(stampModified)).shaped.<>({r=>import r._; _1.map(_=> TendersRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9, _10.get, _11.get, _12, _13.get, _14.get, _15.get, _16.get, _17.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column start_date SqlType(date) */
    val startDate: Rep[java.sql.Date] = column[java.sql.Date]("start_date")
    /** Database column end_date SqlType(date) */
    val endDate: Rep[java.sql.Date] = column[java.sql.Date]("end_date")
    /** Database column number_of_bids SqlType(int4) */
    val numberOfBids: Rep[Int] = column[Int]("number_of_bids")
    /** Database column amount SqlType(float8) */
    val amount: Rep[Double] = column[Double]("amount")
    /** Database column currency SqlType(varchar), Length(10,true) */
    val currency: Rep[String] = column[String]("currency", O.Length(10,varying=true))
    /** Database column tax_included SqlType(bool) */
    val taxIncluded: Rep[Boolean] = column[Boolean]("tax_included")
    /** Database column title SqlType(varchar), Length(255,true) */
    val title: Rep[String] = column[String]("title", O.Length(255,varying=true))
    /** Database column description SqlType(text), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column zpu_id SqlType(varchar), Length(255,true) */
    val zpuId: Rep[String] = column[String]("zpu_id", O.Length(255,varying=true))
    /** Database column link SqlType(varchar), Length(255,true) */
    val link: Rep[String] = column[String]("link", O.Length(255,varying=true))
    /** Database column lots_text SqlType(text), Default(None) */
    val lotsText: Rep[Option[String]] = column[Option[String]]("lots_text", O.Default(None))
    /** Database column author_company SqlType(varchar), Length(255,true) */
    val authorCompany: Rep[String] = column[String]("author_company", O.Length(255,varying=true))
    /** Database column telephone SqlType(varchar), Length(255,true) */
    val telephone: Rep[String] = column[String]("telephone", O.Length(255,varying=true))
    /** Database column status SqlType(varchar), Length(255,true) */
    val status: Rep[String] = column[String]("status", O.Length(255,varying=true))
    /** Database column is_commercial SqlType(bool) */
    val isCommercial: Rep[Boolean] = column[Boolean]("is_commercial")
    /** Database column stamp_modified SqlType(int8) */
    val stampModified: Rep[Long] = column[Long]("stamp_modified")
  }
  /** Collection-like TableQuery object for table Tenders */
  lazy val Tenders = new TableQuery(tag => new Tenders(tag))

  /** Entity class storing rows of table TgAddresses
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param chatId Database column chat_id SqlType(int4), Default(None)
   *  @param lang Database column lang SqlType(varchar), Length(10,true), Default(None)
   *  @param partnerDbId Database column partner_db_id SqlType(int4), Default(None)
   *  @param username Database column username SqlType(varchar), Length(255,true), Default(None) */
  case class TgAddressesRow(id: Int, chatId: Option[Int] = None, lang: Option[String] = None, partnerDbId: Option[Int] = None, username: Option[String] = None)
  /** GetResult implicit for fetching TgAddressesRow objects using plain SQL queries */
  implicit def GetResultTgAddressesRow(implicit e0: GR[Int], e1: GR[Option[Int]], e2: GR[Option[String]]): GR[TgAddressesRow] = GR{
    prs => import prs._
    TgAddressesRow.tupled((<<[Int], <<?[Int], <<?[String], <<?[Int], <<?[String]))
  }
  /** Table description of table tg_addresses. Objects of this class serve as prototypes for rows in queries. */
  class TgAddresses(_tableTag: Tag) extends Table[TgAddressesRow](_tableTag, "tg_addresses") {
    def * = (id, chatId, lang, partnerDbId, username) <> (TgAddressesRow.tupled, TgAddressesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), chatId, lang, partnerDbId, username).shaped.<>({r=>import r._; _1.map(_=> TgAddressesRow.tupled((_1.get, _2, _3, _4, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column chat_id SqlType(int4), Default(None) */
    val chatId: Rep[Option[Int]] = column[Option[Int]]("chat_id", O.Default(None))
    /** Database column lang SqlType(varchar), Length(10,true), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(10,varying=true), O.Default(None))
    /** Database column partner_db_id SqlType(int4), Default(None) */
    val partnerDbId: Rep[Option[Int]] = column[Option[Int]]("partner_db_id", O.Default(None))
    /** Database column username SqlType(varchar), Length(255,true), Default(None) */
    val username: Rep[Option[String]] = column[Option[String]]("username", O.Length(255,varying=true), O.Default(None))

    /** Foreign key referencing Companies (database name tg_addresses_companies_fk) */
    lazy val companiesFk = foreignKey("tg_addresses_companies_fk", partnerDbId, Companies)(r => Rep.Some(r.companyId), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table TgAddresses */
  lazy val TgAddresses = new TableQuery(tag => new TgAddresses(tag))

  /** Entity class storing rows of table UsersChats
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param chatId Database column chat_id SqlType(int8)
   *  @param userId Database column user_id SqlType(int8), Default(None)
   *  @param telephone Database column telephone SqlType(varchar), Length(15,true)
   *  @param contragentId Database column contragent_id SqlType(int4), Default(None)
   *  @param firstName Database column first_name SqlType(varchar), Length(50,true)
   *  @param lastName Database column last_name SqlType(varchar), Length(50,true), Default(None)
   *  @param dateRegistred Database column date_registred SqlType(date) */
  case class UsersChatsRow(id: Int, chatId: Long, userId: Option[Long] = None, telephone: String, contragentId: Option[Int] = None, firstName: String, lastName: Option[String] = None, dateRegistred: java.sql.Date)
  /** GetResult implicit for fetching UsersChatsRow objects using plain SQL queries */
  implicit def GetResultUsersChatsRow(implicit e0: GR[Int], e1: GR[Long], e2: GR[Option[Long]], e3: GR[String], e4: GR[Option[Int]], e5: GR[Option[String]], e6: GR[java.sql.Date]): GR[UsersChatsRow] = GR{
    prs => import prs._
    UsersChatsRow.tupled((<<[Int], <<[Long], <<?[Long], <<[String], <<?[Int], <<[String], <<?[String], <<[java.sql.Date]))
  }
  /** Table description of table users_chats. Objects of this class serve as prototypes for rows in queries. */
  class UsersChats(_tableTag: Tag) extends Table[UsersChatsRow](_tableTag, "users_chats") {
    def * = (id, chatId, userId, telephone, contragentId, firstName, lastName, dateRegistred) <> (UsersChatsRow.tupled, UsersChatsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(chatId), userId, Rep.Some(telephone), contragentId, Rep.Some(firstName), lastName, Rep.Some(dateRegistred)).shaped.<>({r=>import r._; _1.map(_=> UsersChatsRow.tupled((_1.get, _2.get, _3, _4.get, _5, _6.get, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column chat_id SqlType(int8) */
    val chatId: Rep[Long] = column[Long]("chat_id")
    /** Database column user_id SqlType(int8), Default(None) */
    val userId: Rep[Option[Long]] = column[Option[Long]]("user_id", O.Default(None))
    /** Database column telephone SqlType(varchar), Length(15,true) */
    val telephone: Rep[String] = column[String]("telephone", O.Length(15,varying=true))
    /** Database column contragent_id SqlType(int4), Default(None) */
    val contragentId: Rep[Option[Int]] = column[Option[Int]]("contragent_id", O.Default(None))
    /** Database column first_name SqlType(varchar), Length(50,true) */
    val firstName: Rep[String] = column[String]("first_name", O.Length(50,varying=true))
    /** Database column last_name SqlType(varchar), Length(50,true), Default(None) */
    val lastName: Rep[Option[String]] = column[Option[String]]("last_name", O.Length(50,varying=true), O.Default(None))
    /** Database column date_registred SqlType(date) */
    val dateRegistred: Rep[java.sql.Date] = column[java.sql.Date]("date_registred")
  }
  /** Collection-like TableQuery object for table UsersChats */
  lazy val UsersChats = new TableQuery(tag => new UsersChats(tag))
}
