package com.malliina.http4s

import cats.effect.{IO, Resource, Sync}
import com.malliina.database.Conf
import com.malliina.http.FullUrl
import com.malliina.http.UrlSyntax.url
import com.malliina.http.io.{HttpClientF2, HttpClientIO}
import com.malliina.pill.PillConf
import com.malliina.values.Password
import munit.AnyFixture
import org.http4s.server.Server

trait MUnitDatabaseSuite:
  self: munit.CatsEffectSuite =>
  val db: Fixture[Conf] = new Fixture[Conf]("database"):
    private var conf: Option[Conf] = None
    def apply(): Conf = conf.get

    override def beforeAll(): Unit =
      conf = testConf.toOption.map(_.db)
    override def afterAll(): Unit = ()

  private def testDatabaseConf(password: Password) = Conf(
    url"jdbc:mariadb://127.0.0.1:3306/testapi",
    "testapi",
    password,
    PillConf.mariaDbDriver,
    maxPoolSize = 2,
    autoMigrate = true
  )

  def testConf = PillConf.from(
    PillConf.local("test-pill.conf").resolve().getConfig("pill"),
    isTest = true,
    testDatabaseConf
  )

  override def munitFixtures: Seq[AnyFixture[?]] = Seq(db)

case class ServerTools(server: Server, client: HttpClientF2[IO]):
  def port = server.address.getPort
  def baseHttpUrl = FullUrl("http", s"localhost:$port", "")

trait ServerSuite extends MUnitDatabaseSuite with ServerResources:
  self: munit.CatsEffectSuite =>
  val ember: Resource[IO, ServerTools] = for
//    _ <- Resource.eval(IO.delay(LogbackUtils.init(rootLevel = Level.INFO)))
    conf <- Resource.eval(Sync[IO].fromEither(testConf))
    server <- emberServer[IO](conf)
    client <- HttpClientIO.resource[IO]
  yield ServerTools(server, client)
  val server = ResourceSuiteLocalFixture("server", ember)

  override def munitFixtures: Seq[AnyFixture[?]] = Seq(db, server)
