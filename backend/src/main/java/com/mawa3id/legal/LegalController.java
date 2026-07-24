package com.mawa3id.legal;

import com.mawa3id.common.ResourceNotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serves the public Privacy Policy and Terms of Service as self-contained HTML,
 * localized to English, French and Arabic. These pages are the publicly-hostable
 * URLs the App Store and Play Store require; they are unauthenticated (see the
 * {@code /legal/**} permitAll rule in {@code SecurityConfig}).
 *
 * <p>Language is chosen from the {@code ?lang=} query parameter when it names a
 * supported locale, otherwise from the {@code Accept-Language} header, otherwise
 * English. Arabic is rendered right-to-left.
 *
 * <p>The per-language body fragments live under {@code resources/legal/<lang>/}
 * and are wrapped here in a shared, minimally-styled shell. Fragments are read
 * once at startup so a missing file fails fast rather than 500-ing a visitor.
 */
@RestController
@RequestMapping("/legal")
public class LegalController {

    /** Supported languages, first is the default; order drives the index page. */
    static final List<String> LANGUAGES = List.of("en", "fr", "ar");
    /** Documents served per language. */
    static final List<String> DOCUMENTS = List.of("privacy", "terms");
    private static final String DEFAULT_LANG = "en";

    private static final Map<String, String> LANG_NAMES = Map.of(
            "en", "English", "fr", "Français", "ar", "العربية");
    private static final Map<String, String> DOC_TITLES = Map.of(
            "privacy", "Privacy Policy", "terms", "Terms of Service");

    /** Wrapped, ready-to-serve HTML keyed by "<lang>/<doc>". Immutable after construction. */
    private final Map<String, String> pages = new LinkedHashMap<>();

    public LegalController() {
        for (String lang : LANGUAGES) {
            for (String doc : DOCUMENTS) {
                String fragment = readFragment(lang, doc);
                pages.put(key(lang, doc), wrap(lang, doc, fragment));
            }
        }
    }

    /** Index linking every document in every language. */
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String index(@RequestParam(required = false) String lang,
                        @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String resolved = resolveLang(lang, acceptLanguage);
        boolean rtl = "ar".equals(resolved);
        StringBuilder body = new StringBuilder("<h1>Mawa3id — Legal</h1>\n<ul>\n");
        for (String doc : DOCUMENTS) {
            body.append("<li><a href=\"/legal/").append(doc).append("?lang=").append(resolved)
                    .append("\">").append(DOC_TITLES.get(doc)).append("</a></li>\n");
        }
        body.append("</ul>\n<p>Language:\n");
        for (String l : LANGUAGES) {
            body.append(" <a href=\"/legal?lang=").append(l).append("\">")
                    .append(LANG_NAMES.get(l)).append("</a>");
        }
        body.append("\n</p>\n");
        return shell(resolved, rtl, "Legal", body.toString());
    }

    @GetMapping(value = "/{document}", produces = MediaType.TEXT_HTML_VALUE)
    public String document(@PathVariable String document,
                           @RequestParam(required = false) String lang,
                           @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        if (!DOCUMENTS.contains(document)) {
            throw new ResourceNotFoundException("Unknown legal document: " + document);
        }
        String resolved = resolveLang(lang, acceptLanguage);
        return pages.get(key(resolved, document));
    }

    /**
     * Picks a supported language: an explicit {@code ?lang=} wins, then the first
     * supported tag in {@code Accept-Language}, then the default.
     */
    static String resolveLang(String lang, String acceptLanguage) {
        if (lang != null) {
            String normalized = lang.trim().toLowerCase(Locale.ROOT);
            if (LANGUAGES.contains(normalized)) {
                return normalized;
            }
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            for (Locale.LanguageRange range : safeParse(acceptLanguage)) {
                String tag = range.getRange().toLowerCase(Locale.ROOT);
                String primary = tag.split("-", 2)[0];
                if (LANGUAGES.contains(primary)) {
                    return primary;
                }
            }
        }
        return DEFAULT_LANG;
    }

    private static List<Locale.LanguageRange> safeParse(String acceptLanguage) {
        try {
            return Locale.LanguageRange.parse(acceptLanguage);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private static String key(String lang, String doc) {
        return lang + "/" + doc;
    }

    private static String readFragment(String lang, String doc) {
        ClassPathResource resource = new ClassPathResource("legal/" + lang + "/" + doc + ".html");
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Missing legal fragment: " + resource.getPath(), ex);
        }
    }

    private String wrap(String lang, String doc, String fragment) {
        boolean rtl = "ar".equals(lang);
        return shell(lang, rtl, DOC_TITLES.get(doc), fragment);
    }

    private static String shell(String lang, boolean rtl, String title, String body) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"" + lang + "\" dir=\"" + (rtl ? "rtl" : "ltr") + "\">\n"
                + "<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<meta name=\"robots\" content=\"index, follow\">\n"
                + "<title>Mawa3id — " + title + "</title>\n"
                + "<style>\n"
                + ":root { color-scheme: light dark; }\n"
                + "body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;\n"
                + "  line-height: 1.65; max-width: 46rem; margin: 0 auto; padding: 2rem 1.25rem 4rem;\n"
                + "  color: #1a1a1a; background: #ffffff; }\n"
                + "@media (prefers-color-scheme: dark) { body { color: #e6e6e6; background: #121212; } }\n"
                + "h1 { color: #1E8E5A; font-size: 1.75rem; }\n"
                + "h2 { margin-top: 2rem; font-size: 1.2rem; }\n"
                + "a { color: #1E8E5A; }\n"
                + "hr { border: none; border-top: 1px solid rgba(128,128,128,.3); margin: 2.5rem 0; }\n"
                + "code { background: rgba(128,128,128,.15); padding: .1em .35em; border-radius: 4px; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n" + body + "\n</body>\n</html>\n";
    }
}
