# README
The project uses Spring Boot with Reactor Netty as webserver, this setup was chosen<br>
to be able to handle the ssl/tls tcp connection between the proxy server and producer<br>
with the same dependency

## Details for manual testing
### Effective properties
 - hu.bbox.proxy.host defaults to 0.0.0.0 in proxy and localhost in proxy
 - hu.bbox.proxy.producer.port defaults to 45820
 - hu.bbox.proxy.ssl defaults to true

The proxy server has to be started before the producer since there is no logic implemented<br>
in the producer to wait for the proxy server

By default all components are staaddrting with using ssl/tls, the http server starts on port 8443