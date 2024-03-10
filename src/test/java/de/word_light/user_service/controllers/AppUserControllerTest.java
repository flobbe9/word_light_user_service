package de.word_light.user_service.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.word_light.user_service.entities.AppUser;
import de.word_light.user_service.enums.AppUserRole;
import de.word_light.user_service.exception.ApiException;
import de.word_light.user_service.exception.ApiExceptionFormat;
import de.word_light.user_service.services.AppUserService;


@WebMvcTest(AppUserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AppUserControllerTest {

    @MockBean
    private AppUserService appUserService;

    @Autowired
    private MockMvc mockMvc;

    private String requestMapping = "/api/appUser";

    private AppUser appUser;
    private String email = "max.mustermann@domain.com";
    private String password = "Abc123,.";
    private AppUserRole role = AppUserRole.USER;
    
    private String token = UUID.randomUUID().toString();


    @BeforeEach
    void setup() {

        this.appUser = new AppUser(this.email, this.password, this.role);
    }


    @Test
    void register_shouldBeStatus200() throws Exception {

        MvcResult mvcResult = performPost("/register", this.appUser)
                            .andExpect(status().isCreated())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();

        assertTrue(jsonResponse.contains("\"error\":null"));

        // modify response for the sake of check method
        jsonResponse = jsonResponse.replace("null", "\"Created\"");
        checkJsonApiExceptionFormat(jsonResponse, HttpStatus.CREATED);
    }


    @Test
    void register_shouldBeStatus400_null() throws Exception {

        MvcResult mvcResult = performPost("/register", null)
                             .andExpect(status().isBadRequest())
                             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                             .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_shouldBeStatus400_invalidAppUser() throws Exception {

        this.appUser.setEmail("falsyEmail.com");

        MvcResult mvcResult = performPost("/register", this.appUser)
                             .andExpect(status().isBadRequest())
                             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                             .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);
    }


    @Test
    void update_shouldBeStatus200() throws Exception {

        MvcResult mvcResult = performPut("/update", this.appUser)
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();

        assertTrue(jsonResponse.contains("\"error\":null"));

        // modify response for the sake of check method
        jsonResponse = jsonResponse.replace("null", "\"OK\"");
        checkJsonApiExceptionFormat(jsonResponse, HttpStatus.OK);
    }


    @Test
    void update_shouldBeStatus400_null() throws Exception {

        MvcResult mvcResult = performPut("/update", null)
                             .andExpect(status().isBadRequest())
                             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                             .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_shouldBeStatus400_invalidAppUser() throws Exception {

        this.appUser.setEmail("falsyEmail.com");

        MvcResult mvcResult = performPut("/update", this.appUser)
                             .andExpect(status().isBadRequest())
                             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                             .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);
    }


    @Test
    void confirmAccount_shouldBeStatus200() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", this.email);
        params.add("token", this.token);

        MvcResult mvcResult = performGet("/confirmAccount?", params)
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();

        assertTrue(jsonResponse.contains("\"error\":null"));

        // modify response for the sake of check method
        jsonResponse = jsonResponse.replace("null", "\"OK\"");
        checkJsonApiExceptionFormat(jsonResponse, HttpStatus.OK);
    }


    @Test
    void confirmAccount_shouldBeStatus400_blank() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", "");
        params.add("token", this.token);

        MvcResult mvcResult = performGet("/confirmAccount?", params)
                             .andExpect(status().isBadRequest())
                             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                             .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);

        params.clear();
        params.add("email", this.email);
        params.add("token", "");

        mvcResult = performGet("/confirmAccount?", params)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);
    }


    @Test
    void getByEmail_shouldBeStatus200() throws Exception {

        when(this.appUserService.loadUserByUsername(this.email)).thenReturn(this.appUser);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", this.email);

        performGet("/getByEmail?", params)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(objectToJson(this.appUser)))
                    .andReturn();
    }


    @Test
    void getByEmail_shouldBeStatus400_blank() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", "");

        MvcResult mvcResult = performGet("/getByEmail?", params)
                             .andExpect(status().isBadRequest())
                             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                             .andReturn();

        checkJsonApiExceptionFormat(mvcResult.getResponse().getContentAsString(), HttpStatus.BAD_REQUEST);
    }


    private ResultActions performPost(String path, Object body) throws Exception {

        return this.mockMvc.perform(post(this.requestMapping + path)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectToJson(body)));
    }
    

    private ResultActions performPut(String path, Object body) throws Exception {

        return this.mockMvc.perform(put(this.requestMapping + path)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectToJson(body)));
    }


    private ResultActions performGet(String path, MultiValueMap<String, String> params) throws Exception {

        return this.mockMvc.perform(get(this.requestMapping + path)
                                    .params(params)
                                    .contentType(MediaType.APPLICATION_JSON));
    }


    private String objectToJson(Object object) {

        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

        try {
            return objectWriter.writeValueAsString(object);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new ApiException("Failed to convert object to json String.", e);
        }
    }


    private JsonNode jsonToNode(String json) {

        ObjectReader objectReader = new ObjectMapper().reader();

        try {
            return objectReader.readTree(json);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException("Failed to convert object to json String.", e);
        }
    }


    /**
     * Assert that given json String is an {@link ApiExceptionFormat} object. Also check the {@code status} and {@code error} values match
     * the given HttpStatus.
     * 
     * @param json String formatted as json to check
     * @param status http status
     */
    private void checkJsonApiExceptionFormat(String json, HttpStatus status) {

        JsonNode jsonNode = jsonToNode(json);

        assertEquals(4, jsonNode.size());

        assertTrue(jsonNode.has("status"));
        assertTrue(jsonNode.has("error"));
        assertTrue(jsonNode.has("message"));
        assertTrue(jsonNode.has("path"));

        assertEquals(status.value(), jsonNode.get("status").asInt());
        assertEquals(status.getReasonPhrase(), jsonNode.get("error").asText());
    }
}