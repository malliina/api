package com.malliina.pill

import cats.Monad
import com.malliina.http.HttpClient
import com.malliina.push.apns.*
import cats.implicits.*
import java.nio.file.Path

case class APNSResult(token: APNSToken, result: Either[APNSError, APNSIdentifier]):
  def isTokenBad: Boolean = result.left.exists(_ == BadDeviceToken)

object Push:
  // Can be used across apps
  val keyId = KeyId("S55Y3KS7Y8")
  // Specific to developer account
  val teamId = TeamId("D2T2QC36Z9")
  // App bundle id
  val topic = APNSTopic("com.skogberglabs.pill")

  def default[F[+_]: Monad](file: Path, http: HttpClient[F]): Push[F] =
    new Push(APNSTokenConf(file, keyId, teamId), http)

class Push[F[+_]: Monad](conf: APNSTokenConf, http: HttpClient[F]) extends PushService[F]:
  private val client = APNSHttpClientF(conf, http, isSandbox = false)
//  private val message = APNSMessage(APSPayload.background(None, None, None))
//  private val req = APNSRequest.withTopic(Push.topic, message)

  def push(msg: APNSRequest, to: APNSToken): F[APNSResult] =
    client.push(to, msg).map(result => APNSResult(to, result))

trait PushService[F[+_]]:
  def push(msg: APNSRequest, to: APNSToken): F[APNSResult]

object PushService:
  def noop[F[+_]: Monad]: PushService[F] = new PushService[F]:
    override def push(msg: APNSRequest, to: APNSToken): F[APNSResult] =
      val result = APNSResult(to, Left(APNSError.default("Not implemented.")))
      Monad[F].pure(result)
