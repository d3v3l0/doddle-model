package io.picnicml.doddlemodel.preprocessing

import breeze.linalg.*
import io.picnicml.doddlemodel.data.Feature.FeatureIndex
import io.picnicml.doddlemodel.data.Features
import io.picnicml.doddlemodel.preprocessing.Norms.{L2Norm, Norm}
import io.picnicml.doddlemodel.typeclasses.Transformer

case class Normalizer(normFunction: Norm = L2Norm, private val featureIndex: FeatureIndex)

/** An immutable preprocessor that normalizes rows to unit norm according to specified norm function.
  * See [[io.picnicml.doddlemodel.preprocessing.Norms]] for supported norm functions.
  *
  * @example Scale rows to unit norm according to L2 norm.
  *   {{{
  *     import io.picnicml.doddlemodel.preprocessing.Normalizer.ev
  *     import io.picnicml.doddlemodel.preprocessing.Norms.L2Norm
  *
  *     val featureIndex = FeatureIndex(List(NumericalFeature, NumericalFeature, NumericalFeature))
  *     val x = DenseMatrix(
  *       List(1.0, 2.0, 2.0),
  *       List(-2.0, 0.0, 0.0)
  *     )
  *     val l2Normalizer = Normalizer(L2Norm, featureIndex)
  *     // Note: no fitting required
  *     val xNormalized = ev.transform(l2Normalizer, x)
  *   }}}
  * */
object Normalizer {

  implicit lazy val ev: Transformer[Normalizer] = new Transformer[Normalizer] {

    override def isFitted(model: Normalizer): Boolean = true

    override def fit(model: Normalizer, x: Features): Normalizer = model

    override protected def featureIndexSafe(model: Normalizer): FeatureIndex = model.featureIndex

    override protected def transformSafe(model: Normalizer, x: Features): Features = {
      val rowNorms = model.normFunction(x)
      // no-op for zero vector
      rowNorms(rowNorms :== 0.0) := 1.0
      x(::, *) /:/ rowNorms
    }
  }
}
