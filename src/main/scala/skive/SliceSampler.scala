package skive

import breeze.linalg.{normalize, *, DenseMatrix, DenseVector}
import breeze.numerics.log
import breeze.stats.distributions.Uniform
import breeze.stats.sampling.standardBasis


/**
 * A Monte Carlo Markov Chain for sampling multi-dimensional values given a
 * log-likelihood function over the sample space.
 *
 * @param logLikelihood The log-likelihood over the sample space. Potentially unnormalized.
 * @param init Starting point in the sample space.
 * @param burnin Number of samples to throwaway before starting.
 * @param thin Thins the sequence by skipping 'thin' samples each iteration.
 * @param componentwise Whether slices are made independently for each component.
 * @param stepSize The size of the interval upon which the bounds of the slice are iteratively determined.
 */
class SliceSampler(logLikelihood:(DenseVector[Double])=>Double, init:DenseVector[Double], burnin:Int=0, thin:Int=0, componentwise:Boolean=true, stepSize:Double=1)
  extends Iterator[Sample] {

  /* The last sampled value or, before 'next' has been called, the starting point. Mutable. */
  protected var current = Sample(init)

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
    val logSliceHeight = log(uniform.draw) + logLikelihood(initial.value) //log(uniform.draw * exp(logLikelihood(initial.sample)))

    /* The distance forwards and backwards composing the bounds of the slice. Initially of width 'stepSize'. */
    val sliceOffset = uniform.draw
    val distanceBounds = ( stepOut(logSliceHeight, direction, upperBound=false, (sliceOffset - 1) * stepSize),
                           stepOut(logSliceHeight, direction, upperBound=true, sliceOffset * stepSize))

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
    val candidateDist = uniform.draw * (distanceBounds._2 - distanceBounds._1) + distanceBounds._1
    val candidateSample = direction * candidateDist + initial.value
    val candidateLogLikelihood = logLikelihood(candidateSample)
    if (logSliceHeight > candidateLogLikelihood) {
      val newBounds = if (candidateDist < 0) (candidateDist, distanceBounds._2) else (distanceBounds._1, candidateDist)
      stepIn(initial, direction, logSliceHeight, newBounds)
    } else Sample(candidateSample, Some(candidateLogLikelihood))
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
  protected def stepOut(logSliceHeight:Double, direction:DenseVector[Double], upperBound:Boolean, distance:Double):Double = {
    if (logSliceHeight < offsetLogLikelihood(direction, distance))
      stepOut(logSliceHeight, direction, upperBound, distance + (if (upperBound) stepSize else -stepSize))
    else
      distance
  }

  /**
   * The log-likelihood of a new point offset from the last sampled value.
   *
   * @param direction The direction in the sample space to move.
   * @param distance The distance from the current sample to move along the given direction.
   */
  protected def offsetLogLikelihood(direction:DenseVector[Double], distance:Double) =
    logLikelihood(current.value + direction * distance)
}


/**
 * Represents a sampled value and, optionally, the computed log likelihood
 */
case class Sample(value:DenseVector[Double], logLikelihood:Option[Double]=None)
