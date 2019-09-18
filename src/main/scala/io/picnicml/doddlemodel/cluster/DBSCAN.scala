package io.picnicml.doddlemodel.cluster

import breeze.linalg.functions.euclideanDistance
import cats.syntax.option._
import io.picnicml.doddlemodel.data.Features
import io.picnicml.doddlemodel.typeclasses.Clusterer

/** An immutable DBSCAN model.
  *
  * @param eps: the maximum distance between points in a group
  * @param minSamples: the minimum number of point in a core group
  *
  * Examples:
  * val model = DBSCAN()
  * val model = DBSCAN(eps = 1.5)
  * val model = DBSCAN(minSamples = 3)
  * val model = DBSCAN(eps = 2.0, minSamples = 3)
  */
case class DBSCAN private(eps: Double, minSamples: Int, private val label: Option[Array[Int]])

object DBSCAN {

  def apply(eps: Double = 1.0, minSamples: Int = 1): DBSCAN = {
    require(eps > 0.0, "Maximum distance needs to be larger than 0")
    require(minSamples > 0, "Minimum number of samples needs to be larger than 0")
    DBSCAN(eps, minSamples, none)
  }

  implicit lazy val ev: Clusterer[DBSCAN] = new Clusterer[DBSCAN] {

    override def isFitted(model: DBSCAN): Boolean = model.label.isDefined

    override protected def labelSafe(model: DBSCAN): Array[Int] = model.label.get

    override protected def copy(model: DBSCAN): DBSCAN =
      model.copy()

    override protected def copy(model: DBSCAN, label: Array[Int]): DBSCAN =
      model.copy(label = label.some)

    override protected def fitSafe(model: DBSCAN, x: Features): DBSCAN = {
      val label = Array.fill[Int](x.rows)(Int.MaxValue)
      var groupId = -1
      for (pointId <- 0 until x.rows if label(pointId) == Int.MaxValue) {
        var groupQueue = findNeighbors(pointId, x, model.eps)
        if (groupQueue.size + 1 < model.minSamples) {
          label(pointId) = -1
        } else {
          groupId += 1
          label(pointId) = groupId
          while (groupQueue.size > 0) {
            val tmpGroupQueue = groupQueue
            groupQueue = Set[Int]()
            tmpGroupQueue.foreach { i =>
              if (label(i) == -1) label(i) = groupId
              else if (label(i) == Int.MaxValue) {
                label(i) = groupId
                val neighbors = findNeighbors(i, x, model.eps)
                if (neighbors.size + 1 < model.minSamples)
                  groupQueue ++= neighbors
              }
            }
          }
        }
      }
      copy(model, label)
    }

    override protected def fitPredictSafe(model: DBSCAN, x: Features): Array[Int] =
      labelSafe(fitSafe(model, x))

    private def findNeighbors(pointId: Int, x: Features, eps: Double): Set[Int] =
      (0 until x.rows).filter { i =>
        i != pointId && euclideanDistance(x(i, ::).t, x(pointId, ::).t) <= eps
      }.toSet
  }
}
