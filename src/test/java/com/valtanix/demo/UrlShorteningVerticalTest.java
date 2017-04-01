package com.valtanix.demo;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by kumaran on 3/31/17.
 */
@RunWith(VertxUnitRunner.class)
public class UrlShorteningVerticalTest {
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(UrlShorteningVertical.class.getName(),context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

            vertx.createHttpClient().getNow(8080, "localhost", "/status",
                    response -> {
                        response.handler(body -> {
                            context.assertTrue(body.toString().contains("All is well!"));
                            async.complete();
                        });
                    });
        }
}