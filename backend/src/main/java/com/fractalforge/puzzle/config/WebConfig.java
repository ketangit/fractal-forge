package com.fractalforge.puzzle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the statically-exported Next.js frontend from the classpath and adds
 * SPA-style fallbacks: "/designer" resolves to designer.html, unknown paths
 * fall back to index.html. API routes are untouched.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/").resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						Resource html = location.createRelative(resourcePath + ".html");
						if (html.exists() && html.isReadable()) {
							return html;
						}
						Resource index = location.createRelative("index.html");
						return index.exists() && index.isReadable() ? index : null;
					}
				});
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// Browsers attach an Origin header to every POST, and behind the
		// Cloud Run / Firebase Hosting proxies the container sees
		// http://<host>:8080, so even "same-origin" calls fail Spring's
		// origin check unless the public origins are allowed here (the
		// forwarded-header strategy in application.properties covers the
		// scheme, but not the Hosting preview-channel hosts).
		// Only the verbs the API actually serves; no PUT/DELETE endpoints exist.
		registry.addMapping("/api/**")
				.allowedOriginPatterns("https://craftedbyk.com", "https://www.craftedbyk.com",
						"https://craftedbyk-prod.web.app", "https://craftedbyk-prod--*.web.app",
						"https://craftedbyk-prod.firebaseapp.com", "https://fractal-forge-csmuhhgvva-ue.a.run.app",
						"http://localhost:3000")
				.allowedMethods("GET", "POST");
	}
}
