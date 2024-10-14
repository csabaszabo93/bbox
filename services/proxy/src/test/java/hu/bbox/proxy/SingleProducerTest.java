package hu.bbox.proxy;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;

@RunWith(Cucumber.class)
@CucumberOptions(features = "classpath:features/single_producer.feature", glue = "hu.bbox.proxy.stepdefs.singleproducer")
class SingleProducerTest {
}
