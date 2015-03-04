package skive

import SliceSampler._

import breeze.linalg.{normalize, *, DenseMatrix, DenseVector}
import breeze.numerics.log
import breeze.stats.distributions.Uniform
import breeze.stats.sampling.standardBasis

/**
 * A Monte Carlo Markov Chain for sampling multi-dimensional values given a
 * log-likelihood function over the sample space.
 *
 * @param logLikelihoodFunc The log-likelihood over the sample space. Potentially unnormalized.
 * @param init Starting point in the sample space.
 * @param burnin Number of samples to throwaway before starting.
 * @param thin Thins the sequence by skipping 'thin' samples each iteration.
 * @param componentwise Whether slices are made independently for each component.
 * @param stepSize The size of the interval upon which the bounds of the slice are iteratively determined.
 */
class SliceSampler(logLikelihoodFunc:LogLikelihood, init:DenseVector[Double], burnin:Int=0, thin:Int=0, componentwise:Boolean=true, stepSize:Double=1)
  extends Iterator[Sample] {

  /* The last sampled value or, before 'next' has been called, the starting point. Mutable. */
  protected var current = Sample(init, Right(logLikelihoodFunc))

  /* The number of dimensions being sampled */
  protected val dims = init.length

  protected val uniform = new Uniform(0d, 1d)

  /* Move to a good part of the sample space by burning some samples first */
  for (i <- 1 to burnin) sample

  override def hasNext = true

  /** Returns the next sample, skipping 'thin' samples first */
  override def next = (1 to thin+1) map { i => sample } last

  /** Produces a single sample and advances the iterator */
  protected def sample = {
    current =
      if (componentwise) {
        /* Samples the next value by slicing one component direction at a time */
        val shuffleOrder = standardBasis.permutation(dims).sample
        val components = DenseMatrix.eye[Double](dims)
        val shuffledComponents = shuffleOrder.map(components(*, _).underlying)
        shuffledComponents.foldLeft(current) { case (sample, direction) => slice(sample, direction) }
      } else {
        /* Samples the next value by slicing once in a randomly chosen composite direction */
        slice(current, normalize(DenseVector.rand(dims)))
      }
    current
  }

  /** Draws the next sample given the current point and a direction */
  protected def slice(initial:Sample, direction:DenseVector[Double]):Sample = {
    /* The log of the slice height where the height is sampled between 0 and the value of the likelihood at the initial point */
    val logSliceHeight = log(uniform.draw) + initial.logLikelihood //log(uniform.draw * exp(initial.logLikelihood))

    /* The distance forwards and backwards composing the bounds of the slice. Initially of width 'stepSize'. */
    val sliceOffset = uniform.draw
    val distanceBounds = ( stepOut(initial, logSliceHeight, direction, upperBound=false, (sliceOffset - 1) * stepSize),
                           stepOut(initial, logSliceHeight, direction, upperBound=true, sliceOffset * stepSize))

    /* Find the next sample by selecting a new point in the slice */
    stepIn(initial, direction, logSliceHeight, distanceBounds)
  }

  /**
   * Randomly selects the next sample point along 'direction' anywhere between 'distBackwards' and 'distForwards' from
   * the 'initial' point. Recursively contracts the distance bounds until a sample is found having a log-likelihood
   * greater than the current slice height.
   *
   * @param initial The current location in the sample space.
   * @param direction The direction in the sample space of the slice.
   * @param logSliceHeight The log of the height at which we're slicing.
   * @param distanceBounds The lower and upper bounds of the distance being sampled between.
   * @return A new 'Sample' selected from the slice having log-likelihood greater than the current slice height.
   */
  def stepIn(initial:Sample, direction:DenseVector[Double], logSliceHeight:Double, distanceBounds:(Double, Double)):Sample = {
    val distance = uniform.draw * (distanceBounds._2 - distanceBounds._1) + distanceBounds._1
    val candidate = Sample(direction * distance + initial.value, Right(logLikelihoodFunc))
    if (logSliceHeight > candidate.logLikelihood) {
      val newBounds = if (distance < 0) (distance, distanceBounds._2) else (distanceBounds._1, distance)
      stepIn(initial, direction, logSliceHeight, newBounds)
    } else candidate
  }

  /**
   * Recursively expands a bound of the slice until it envelopes the width of the
   * log-likelihood function at the slice height.
   *
   * @param logSliceHeight The log of the height at which we're slicing.
   * @param direction The direction in the sample space of the slice.
   * @param upperBound Whether we're expanding the upper or lower bound along 'direction'.
   *                   Unless 'distance' is zero this should match it's sign.
   * @param distance The candidate distance being stepped-out from the current sample point along 'direction'.
   * @return The new distance along our direction known to envelop the slice.
   */
  protected def stepOut(initial:Sample, logSliceHeight:Double, direction:DenseVector[Double], upperBound:Boolean, distance:Double):Double = {
    val candidate = direction * distance + initial.value
    if (logSliceHeight < logLikelihoodFunc(candidate))
      stepOut(initial, logSliceHeight, direction, upperBound, distance + (if (upperBound) stepSize else -stepSize))
    else
      distance
  }
}


object SliceSampler {

  type LogLikelihood = (DenseVector[Double] => Double)

  /** Represents a sampled value and it's log-likelihood */
  case class Sample(value:DenseVector[Double], logLikelihoodOrFunc:Either[Double, LogLikelihood]) {
    lazy val logLikelihood = logLikelihoodOrFunc match {
      case Left(ll) => ll
      case Right(llFunc) => llFunc(value)
    }
  }
}
