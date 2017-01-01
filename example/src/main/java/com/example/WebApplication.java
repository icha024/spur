///*
// * Copyright 2016 Ian Chan
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.example;
//
//import io.undertow.Handlers;
//import io.undertow.Undertow;
//import io.undertow.server.HttpHandler;
//import io.undertow.server.HttpServerExchange;
//import io.undertow.util.Headers;
//
//import java.util.logging.Logger;
//import java.util.regex.Pattern;
//
///**
// * Server side application for Google Authenticator's Time-based One Time Password (TOPT)
// *
// * @author Ian Chan
// */
//public class WebApplication {
//
//	private final static Logger LOGGER = Logger.getLogger(WebApplication.class.getName());
//
//	private static final String JSON_CONTENT_TYPE = "application/json";
//	private static final String RESULT_SUCCESS = "{\"result\":\"success\"}";
//	private static final String RESULT_FAILED = "{\"result\":\"failed\"}";
//
//	private static final Pattern KEY_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-=]+$");
//	private static final Pattern CODE_PATTERN = Pattern.compile("^[\\d]+$");
//
//	private TokenCodeValidator totpCodeValidator = new TokenCodeValidator();
//
//	public static void main(final String[] args) {
//		new WebApplication().start();
//	}
//
//	public void start() {
//		Integer port = getEnvProperty("PORT", 8080);
//		Integer pastInterval = getEnvProperty("otp.interval.past", 0);
//		Integer futureInterval = getEnvProperty("otp.interval.future", 0);
//		Integer passcodeLength = getEnvProperty("otp.passcode.length", 6);
//
//		LOGGER.info("Listening to port: " + port);
//		LOGGER.info("Max past interval: " + pastInterval);
//		LOGGER.info("Max future interval: " + futureInterval);
//		LOGGER.info("Passcode length: " + passcodeLength);
//
//		Undertow server = Undertow.builder().addHttpListener(port, "0.0.0.0")
//				.setHandler(Handlers.pathTemplate().add("/{key}/{code}", (AsyncHttpHandler) exchange -> {
//					String key = exchange.getQueryParameters().get("key").getFirst();
//					String code = exchange.getQueryParameters().get("code").getFirst();
//
//					if (!KEY_PATTERN.matcher(key).matches() || !CODE_PATTERN.matcher(code).matches()) {
//						exchange.getResponseSender().send(RESULT_FAILED);
//						LOGGER.info("Invalid input");
//						return;
//					}
//
//					boolean success = totpCodeValidator.validateTOTP(key, code, passcodeLength, pastInterval, futureInterval);
//
//					exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, JSON_CONTENT_TYPE);
//					if (success) {
//						exchange.getResponseSender().send(RESULT_SUCCESS);
//						//LOGGER.info("Verification success.");
//					} else {
//						exchange.getResponseSender().send(RESULT_FAILED);
//						//LOGGER.info("Verification failed.");
//					}
//				})).build();
//		server.start();
//	}
//
//	private Integer getEnvProperty(String propName, Integer defaultVal) {
//		String propVal = System.getenv(propName);
//		return (propVal == null) ? Integer.getInteger(propName, defaultVal) : Integer.parseInt(propVal);
//	}
//
//	@FunctionalInterface
//	interface AsyncHttpHandler extends HttpHandler {
//		default void handleRequest(HttpServerExchange exchange) throws Exception {
//			// non-blocking
//			if (exchange.isInIoThread()) {
//				exchange.dispatch(this);
//				return;
//			}
//			// handler code
//			asyncBlockingHandler(exchange);
//		}
//
//		void asyncBlockingHandler(HttpServerExchange exchange) throws Exception;
//	}
//}
