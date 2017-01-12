# skive
*A multivariate slice sampler for Scala*

Slice sampling is a simple but effective MCMC method for drawing pseudo-random values from a statistical distribution.  Given an evaluable log-pdf of a random variable, or any log-likelihood proportional to the density, *skive* will generate random samples which reflect the shape of the distribution being sampled from.  Compared to other methods, slice sampling is often more efficient and generally performs well without careful tuning.


## Usage

Add *skive* to your sbt dependencies:
```scala
libraryDependencies += "com.github.wsiegenthaler" % "skive_2.12" % "0.8.2"

resolvers += "releases" at "https://oss.sonatype.org/content/repositories/releases"
```

*skive* provides a simple ```Iterator[Sample]``` interface for obtaining samples.  Constructing a sampler requires only a function proportional to the log of the density being sampled and an initial guess from the sample space:
```scala
import skive.SliceSampler
import breeze.linalg._

def logLikelihood(v:DenseVector[Double]):Double = ...
val initial:DenseVector[Double] = ...

val sampler = new SliceSampler(logLikelihood _, initial)
val samples = sampler.take(10)
```
Returned ```Sample``` instances include the sampled value in addition to the cooresponding log-likelihood.  The constructor can also be configured with the following options:
* **burnin** Number of samples to throwaway before starting, allowing the sampler time to find a good part of the sample space. This should be increased when there is low confidence in the initial guess given to the sampler. *[default 0]*
* **thin** Thins the sequence by skipping 'thin' samples each iteration. *[default 0]*
* **componentwise** Whether slices are made independently for each component.  When true, each sample is the result of *n* consecutive slices for each of the *n* dimensions. Otherwise, a single slice in a composite direction is made for each sample. *[default true]*
* **initStep** The initial distance the bounds of the slice are expanded when stepping out. *[default 1e-1]*
* **stepBase** The order of magnitude by which the step size increases when stepping out (i.e. a factor of 1 will keep the step size constant, 2 will double the distance at each step). *[default 2]*


## References

* MacKay, David, FRS. "Approximating Probability Distributions (III): Monte Carlo Methods (II): Slice Sampling." YouTube. Web. 25 Jan. 2015. <https://www.youtube.com/watch?v=Qr6tg9oLGTA>.

* Neal, Radford M. ["Slice Sampling."](http://people.ee.duke.edu/~lcarin/slice.pdf) The Annals of Statistics 31.3 (2003): 705-67. Web.

* ["Slice Sampling."](http://en.wikipedia.org/wiki/Slice_sampling) Wikipedia. Wikimedia Foundation, n.d. Web. 26 Feb. 2015.

* HIPS/Spearmint. Computer software. GitHub. N.p., n.d. Web. 3 Feb. 2015. <http://github.com/HIPS/Spearmint>.


## License

Everything in this repo is BSD License unless otherwise specified

skive (c) 2015 Weston Siegenthaler
