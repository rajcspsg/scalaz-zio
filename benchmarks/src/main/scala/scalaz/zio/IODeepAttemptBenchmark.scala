// Copyright (C) 2017 John A. De Goes. All rights reserved.
package scalaz.zio

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import scala.concurrent.Await

import IOBenchmarks._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class IODeepAttemptBenchmark {
  case class ScalazError(message: String)

  @Param(Array("1000"))
  var depth: Int = _

  def halfway = depth / 2

  @Benchmark
  def thunkDeepAttempt(): BigInt = {
    def descend(n: Int): Thunk[BigInt] =
      if (n == depth) Thunk.fail(new Error("Oh noes!"))
      else if (n == halfway) descend(n + 1).attempt.map(_.fold(_ => 50, a => a))
      else descend(n + 1).map(_ + n)

    descend(0).unsafeRun()
  }

  @Benchmark
  def futureDeepAttempt(): BigInt = {
    import scala.concurrent.Future
    import scala.concurrent.duration.Duration.Inf

    def descend(n: Int): Future[BigInt] =
      if (n == depth) Future.failed(new Exception("Oh noes!"))
      else if (n == halfway) descend(n + 1).recover { case _ => 50 } else descend(n + 1).map(_ + n)

    Await.result(descend(0), Inf)
  }

  @Benchmark
  def monixDeepAttempt(): BigInt = {
    import monix.eval.Task

    def descend(n: Int): Task[BigInt] =
      if (n == depth) Task.raiseError(new Error("Oh noes!"))
      else if (n == halfway) descend(n + 1).attempt.map(_.fold(_ => 50, a => a))
      else descend(n + 1).map(_ + n)

    descend(0).runSyncMaybe.right.get
  }

  @Benchmark
  def scalazDeepAttempt(): BigInt = {
    def descend(n: Int): IO[ScalazError, BigInt] =
      if (n == depth) IO.fail(ScalazError("Oh noes!"))
      else if (n == halfway) descend(n + 1).redeemPure[ScalazError, BigInt](_ => 50, identity)
      else descend(n + 1).map(_ + n)

    unsafeRun(descend(0))
  }

  @Benchmark
  def scalazDeepAttemptBaseline(): BigInt = {
    def descend(n: Int): IO[Error, BigInt] =
      if (n == depth) IO.fail(new Error("Oh noes!"))
      else if (n == halfway) descend(n + 1).redeemPure[Error, BigInt](_ => 50, identity)
      else descend(n + 1).map(_ + n)

    unsafeRun(descend(0))
  }

  @Benchmark
  def catsDeepAttempt(): BigInt = {
    import cats.effect._

    def descend(n: Int): IO[BigInt] =
      if (n == depth) IO.raiseError(new Error("Oh noes!"))
      else if (n == halfway) descend(n + 1).attempt.map(_.fold(_ => 50, a => a))
      else descend(n + 1).map(_ + n)

    descend(0).unsafeRunSync()
  }
}
