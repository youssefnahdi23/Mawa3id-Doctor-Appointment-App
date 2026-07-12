package com.mawa3id.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serialize {@code Page} responses via a stable DTO ({@code content} +
 * {@code page} metadata) rather than the internal {@code PageImpl} shape,
 * so the mobile client can rely on a fixed JSON structure.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
