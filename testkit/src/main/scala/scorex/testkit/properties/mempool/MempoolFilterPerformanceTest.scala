package scorex.testkit.properties.mempool

import java.security.MessageDigest
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}
import scorex.core.transaction.box.proposition.Proposition
import scorex.core.transaction.{MemoryPool, Transaction}
import scorex.core.utils._

trait MempoolFilterPerformanceTest[P <: Proposition, TX <: Transaction[P], MPool <: MemoryPool[TX, MPool]]
  extends PropSpec
    with GeneratorDrivenPropertyChecks
    with Matchers
    with PropertyChecks
    with MemoryPoolTest[P, TX, MPool] {

  var initializedMempool: Option[MPool] = None

  val thresholdInHashes = 500000

  private val HeatJVMHashesCount = 1000000 //to heat up JVM, just in case it is cold

  val thresholdSecs: Double = {
    //heat up
    (1 to HeatJVMHashesCount).foreach(i => MessageDigest.getInstance("SHA-256").digest(("dummy" + i).getBytes()))

    val t0 = System.currentTimeMillis()
    (1 to thresholdInHashes).foreach(i => MessageDigest.getInstance("SHA-256").digest(("dummy" + i).getBytes()))
    val t = System.currentTimeMillis()
    (t - t0) / 1000.0
  }

  property("Mempool should be able to store a lot of transactions") {
    var m: MPool = memPool
    (0 until 1000) foreach { _ =>
      forAll(transactionGenerator) { tx: TX =>
        m = m.put(tx).get
      }
    }
    m.size should be > 1000
    initializedMempool = Some(m)
  }

  property("Mempool filter of non-existing transaction should be fast") {
    val m = initializedMempool.get
    forAll(transactionGenerator) { tx: TX =>
      val (time, _) = profile(m.filter(Seq(tx)))
      assert(time < thresholdSecs)
    }
  }

  property("Mempool filter of existing transaction should be fast") {
    var m = initializedMempool.get
    forAll(transactionGenerator) { tx: TX =>
      m = m.put(tx).get
      val (time, _) = profile(m.filter(Seq(tx)))
      assert(time < thresholdSecs)
    }
  }
}
