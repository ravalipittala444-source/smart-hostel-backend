package com.example.Smart_Hostel;

import com.example.Smart_Hostel.entity.AppUser;
import com.example.Smart_Hostel.repository.AppUserRepository;
import com.example.Smart_Hostel.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testauthdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class AuthIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private com.example.Smart_Hostel.repository.StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void test1_registerValidStudent_andVerifyBCryptHash() throws Exception {
        String json = """
            {
                "fullName": "Rahul Verma",
                "email": "rahul.verma@college.edu",
                "phone": "9876543210",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("rahul.verma@college.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Verify stored in DB
        Optional<AppUser> userOpt = userRepository.findByEmail("rahul.verma@college.edu");
        assertThat(userOpt).isPresent();
        AppUser user = userOpt.get();
        assertThat(user.getFullName()).isEqualTo("Rahul Verma");
        // Verify password is BCrypt hashed, NOT plaintext
        assertThat(user.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();

        // Verify Student profile record was created in students table as well
        Optional<com.example.Smart_Hostel.entity.Student> studentOpt = studentRepository.findByEmail("rahul.verma@college.edu");
        assertThat(studentOpt).isPresent();
        assertThat(studentOpt.get().getFullName()).isEqualTo("Rahul Verma");
    }

    @Test
    void test2_duplicateEmail_isRejectedWithConflict() throws Exception {
        String json = """
            {
                "fullName": "Student One",
                "email": "duplicate@hostel.edu",
                "phone": "9876543210",
                "password": "SecretPassword1"
            }
            """;

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        // Second registration with same email is rejected
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void test3_anonymousUser_cannotRegisterAsAdminOrWarden() throws Exception {
        String jsonAdmin = """
            {
                "fullName": "Fake Admin",
                "email": "fakeadmin@hostel.edu",
                "password": "AdminPassword123!",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(
                    "Public registration only permits STUDENT accounts. Staff roles (ADMIN/WARDEN) must be created by an authorized Administrator."));
    }

    @Test
    void test4_loginWithCorrectPassword_returnsJwtToken() throws Exception {
        AppUser user = AppUser.builder()
                .fullName("Test User")
                .email("test.login@hostel.edu")
                .passwordHash(passwordEncoder.encode("ValidPass123"))
                .role(AppUser.UserRole.STUDENT)
                .status(AppUser.UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        String loginJson = """
            {
                "email": "test.login@hostel.edu",
                "password": "ValidPass123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("test.login@hostel.edu"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void test5_loginWithIncorrectPassword_failsWithUnauthorized() throws Exception {
        AppUser user = AppUser.builder()
                .fullName("Test User")
                .email("test.fail@hostel.edu")
                .passwordHash(passwordEncoder.encode("CorrectPassword"))
                .role(AppUser.UserRole.STUDENT)
                .status(AppUser.UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        String wrongPassJson = """
            {
                "email": "test.fail@hostel.edu",
                "password": "WrongPassword!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongPassJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void test6_unregisteredEmail_failsWithUnauthorized() throws Exception {
        String unknownEmailJson = """
            {
                "email": "nonexistent@hostel.edu",
                "password": "SomePassword"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(unknownEmailJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test7_anonymousUser_cannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    void test8_studentRole_cannotPerformAdminUserOperations() throws Exception {
        AppUser student = AppUser.builder()
                .fullName("Student User")
                .email("student@hostel.edu")
                .passwordHash(passwordEncoder.encode("Pass123"))
                .role(AppUser.UserRole.STUDENT)
                .status(AppUser.UserStatus.ACTIVE)
                .build();
        userRepository.save(student);

        String studentToken = jwtService.generateToken(student);

        String newUserJson = """
            {
                "fullName": "Unauthorized User",
                "email": "unauth@hostel.edu",
                "role": "WARDEN"
            }
            """;

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserJson))
                .andExpect(status().isForbidden());
    }
}

