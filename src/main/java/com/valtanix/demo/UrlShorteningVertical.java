package com.valtanix.demo;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

/**
 * Created by kumaran on 3/31/17.
 */
public class UrlShorteningVertical extends AbstractVerticle{

   private final StatsDClient statsd = new NonBlockingStatsDClient("valtanix", "localhost", 8125);
    Router router = Router.router(vertx);
         RedisOptions redisConfig = new RedisOptions();


    @Override
    public void start(Future<Void> fut) {
        RedisClient redis = RedisClient.create(vertx, redisConfig);
        System.out.println("Redis client "+redis);
        router.route().handler(BodyHandler.create());
        router.route("/status").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type","application/json").setStatusCode(200).end("All is well!");
        });

        router.get("/get/:key").handler(routingContext->{
            long startTime = System.currentTimeMillis();
            String key = routingContext.request().getParam("key"); //Getit from post body
            checkForEmptiness(routingContext, key,"They Key is not found !");
            redis.get(key,url -> {
                if(url.result()!=null) {
                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json").setStatusCode(200)
                            .end("The URL requested is " + url.result());
                    //statsd.recordExecutionTimeToNow("url.shortening.expand,result=success",startTime);
                    //statsd.incrementCounter(key);
                }else{
                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json").setStatusCode(400)
                            .end("Given Key is not found");
                    statsd.recordExecutionTimeToNow("url.shortening.expand,result=failure",startTime);
                }

            });

        });

        router.post("/put/:url").handler(routingContext -> {
            long startTime = System.currentTimeMillis();
            String urlString = routingContext.request().getParam("url"); //Getit from post body
            checkForEmptiness(routingContext, urlString,"URL cannot be empty/Null");
            String token = getUrlToken(urlString);
            redis.set(token,urlString,voidAsyncResult -> {
                HttpServerResponse response = routingContext.response();
                response.putHeader("content-type","application/json").setStatusCode(200)
                        .end(new StringBuilder("Successfully shortened the URL :").append(token).toString());
                //statsd.recordExecutionTimeToNow("url.shortening.expand,result=success",startTime);
                //statsd.incrementCounter(token);

            });
        });



        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8081    /*config().getInteger("server.port")*/, result -> {
                    if (result.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(result.cause());
                    }
                });
    }

    private void checkForEmptiness(RoutingContext routingContext, String url,String errorMsg) {
        if(StringUtil.isNullOrEmpty(url)){
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type","application/json").setStatusCode(400)
                    .end(errorMsg);
        }
    }

    private String getUrlToken(final String urlString) {
        return String.valueOf(Hashing.murmur3_32().hashString(urlString, StandardCharsets.UTF_8));
    }
}