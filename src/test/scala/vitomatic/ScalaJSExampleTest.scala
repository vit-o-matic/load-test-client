package vitomatic

import utest._

object ScalaJSExampleTest extends TestSuite {

  import Background._

  def tests = TestSuite {
    'ScalaJSExample {
      assert(0 == 0)
    }
  }
}
