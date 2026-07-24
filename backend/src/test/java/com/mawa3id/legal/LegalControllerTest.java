package com.mawa3id.legal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The legal pages are the store-required public URLs, so they must be reachable
 * as HTML without authentication and honour the {@code lang} selection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void privacyIsPublicHtmlDefaultingToEnglish() throws Exception {
        mockMvc.perform(get("/legal/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"en\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Privacy Policy")));
    }

    @Test
    void termsIsPublicHtml() throws Exception {
        mockMvc.perform(get("/legal/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Terms of Service")));
    }

    @Test
    void langQueryParamSelectsFrench() throws Exception {
        mockMvc.perform(get("/legal/privacy").param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"fr\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Politique de confidentialité")));
    }

    @Test
    void arabicIsRightToLeft() throws Exception {
        mockMvc.perform(get("/legal/privacy").param("lang", "ar"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dir=\"rtl\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("سياسة الخصوصية")));
    }

    @Test
    void acceptLanguageHeaderSelectsWhenNoParam() throws Exception {
        mockMvc.perform(get("/legal/terms").header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"fr\"")));
    }

    @Test
    void unknownDocumentIsNotFound() throws Exception {
        mockMvc.perform(get("/legal/cookies"))
                .andExpect(status().isNotFound());
    }

    @Test
    void indexLinksBothDocuments() throws Exception {
        mockMvc.perform(get("/legal"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/legal/privacy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/legal/terms")));
    }
}
