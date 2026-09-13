package vn.edu.phuxuan.elib;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import vn.edu.phuxuan.elib.web.GlobalExceptionHandler;
import vn.edu.phuxuan.elib.web.ProblemResponses;
import vn.edu.phuxuan.elib.web.RequestIdFilter;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(mapper)))
                .addFilters(new RequestIdFilter()).build();
    }

    @Test
    void validationErrorsAre400WithoutRejectedValues() throws Exception {
        mvc.perform(post("/test/validate").contentType("application/json").content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.requestId").isNotEmpty()).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void malformedJsonStays400() throws Exception {
        mvc.perform(post("/test/validate").contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void wrongMethodKeeps405AndAllowHeader() throws Exception {
        mvc.perform(get("/test/validate")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"));
    }

    @Test
    void unexpectedFailureHidesExceptionDetails() throws Exception {
        mvc.perform(get("/test/fail")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An internal error occurred. Please try again later."))
                .andExpect(jsonPath("$.exception").doesNotExist()).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class TestController {
        record Input(@NotBlank String name) {}
        @PostMapping("/test/validate") Input validate(@Valid @RequestBody Input input) { return input; }
        @GetMapping("/test/fail") String fail() { throw new IllegalStateException("internal-diagnostic-only"); }
    }
}
