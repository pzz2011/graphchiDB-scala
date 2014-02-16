
// Experiments on the twitter databse

package edu.cmu.akyrola.experiments

import java.util.Locale
import edu.cmu.graphchidb.{GraphChiDatabaseAdmin, GraphChiDatabase}
import edu.cmu.graphchidb.compute.Pagerank
import edu.cmu.graphchi.queries.QueryCallback
import java.{lang, util}
import java.io.{BufferedWriter, FileWriter}
import edu.cmu.graphchidb.queries.internal.SimpleArrayReceiver
import edu.cmu.graphchi.shards.QueryShard

import java.util._
import java.text._
import java.net._

/**
 *
 * @author Aapo Kyrola
 */
object TwitterExperiments {
  val baseFilename = "/Users/akyrola/graphs/DB/twitter/twitter_rv.net"
  val DB = new GraphChiDatabase(baseFilename, numShards=64)
  
  //val pagerankComputation = new Pagerank(DB)
  val sdf = new java.text.SimpleDateFormat("YYYYMMDD_HHmmss")

  DB.initialize()

  println(DB.columns)

  class BitSetOrigIdReceiver(outEdges: Boolean) extends QueryCallback {
    val bitset = new util.BitSet(100000000)
    def immediateReceive() = true
    def receiveEdge(src: Long, dst: Long, edgeType: Byte, dataPtr: Long) = {
      if (outEdges)   bitset.set(DB.internalToOriginalId(dst).toInt)
      else bitset.set(DB.internalToOriginalId(src).toInt)
    }

    def receiveInNeighbors(vertexId: Long, neighborIds: util.ArrayList[lang.Long], edgeTypes: util.ArrayList[lang.Byte], dataPointers: util.ArrayList[lang.Long])= throw new IllegalStateException()
    def receiveOutNeighbors(vertexId: Long, neighborIds: util.ArrayList[lang.Long], edgeTypes: util.ArrayList[lang.Byte], dataPointers: util.ArrayList[lang.Long])= throw new IllegalStateException()

    def size = bitset.cardinality()
  }

  def inAndOutTest(iterations: Int) {
 val r = new java.util.Random(260379)
    var i = 1

    val id = "%s_%s_i%d".format(InetAddress.getLocalHost.getHostName.substring(0,8), sdf.format(new Date()), iterations)

    val qlog = new BufferedWriter(new FileWriter("inout_twitter_%s.tsv".format(id)))
    qlog.write("outsize,outtime,insize,intime\n")


 
    (0 to iterations).foreach ( i => {
      val v = DB.originalToInternalId(math.abs(r.nextLong() % 65000000))
      val inRecv = new SimpleArrayReceiver(outEdges = false, limit=1000000)

      val tInSt = System.nanoTime()
      DB.queryIn(v, 0, inRecv)
      val tIn = System.nanoTime() - tInSt

      val outRecv = new SimpleArrayReceiver(outEdges = true, limit=1000000)

      val tOutSt = System.nanoTime()  
      DB.queryOut(v, 0, outRecv)
      val tOut = System.nanoTime() - tOutSt

       	this.synchronized {
	      qlog.write("%d,%f,".format(outRecv.size, tOut * 0.001))
    	  qlog.write("%d,%f\n".format(inRecv.size, tIn * 0.001))
    	}
      	if (i%1000 == 0) println("%d/%d".format(i, iterations))
     })
    qlog.close()
  }



  def main(args: Array[String]) {
    
 		inAndOutTest(args(0).toInt);

  }

 }