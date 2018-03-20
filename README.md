# Spring Boot + Spring Security + OAuth2 + Swagger 2

Example Spring Boot + Hibernate + Spring Security + OAuth2 + Swagger 2 project for demonstration purposes. 

## Getting started

To run application:

```mvn package && java -jar target\company-structure-spring-security-oauth2-swagger-1.0-SNAPSHOT.jar```

### Prerequisites:
- Java 8
- Maven
- H2/PostgreSQL

It is possible to run application in one of two profiles:
- h2
- postgres

depending on database engine chose for testing. 

### Testing database schema
![database-schema](src/main/docs/db_schema.png)

## Web Security paths

Ignoring security for path related to Swagger functionalities:

```java
@Configuration
@EnableWebSecurity
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
@Import(Encoders.class)
public class ServerSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder userPasswordEncoder;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(userPasswordEncoder);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(
                "/v2/api-docs",
                "/configuration/ui/**",
                "/swagger-resources/**",
                "/configuration/security/**",
                "/swagger-ui.html",
                "/webjars/**");
    }
}
```

## OAuth 2 + Swagger 2 configuration

Declaring SecurityScheme and SecurityContext beans to enabled OAuth 2.0 authorization inside of Swagger:

```java
@Configuration
public class OAuthSwaggerSecurityConfig {

    @Value("${host}")
    private String host;

    private static final String SECURITY_SCHEME_NAME = "spring_oauth";
    private static final String CLIENT_ID = "spring-security-oauth2-read-write-client";
    private static final String CLIENT_SECRET = "spring-security-oauth2-read-write-client-password1234";

    @Bean
    public SecurityConfiguration securityInfo() {
        return SecurityConfigurationBuilder.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .scopeSeparator(" ")
                .build();
    }

    private AuthorizationScope[] scopes() {
        AuthorizationScope[] scopes = {
                new AuthorizationScope("read", "for read operations"),
                new AuthorizationScope("write", "for write operations")
        };

        return scopes;
    }

    private List<SecurityReference> securityReferences() {
        return Lists.newArrayList(new SecurityReference(SECURITY_SCHEME_NAME, scopes()));
    }

    @Bean
    public SecurityScheme securityScheme() {
        LoginEndpoint loginEndpoint = new LoginEndpoint(host + "/oauth/authorize");
        GrantType grantType = new ImplicitGrant(loginEndpoint, OAuth2AccessToken.ACCESS_TOKEN);

        SecurityScheme oauth = new OAuthBuilder().name(SECURITY_SCHEME_NAME)
                .grantTypes(Lists.newArrayList(grantType))
                .scopes(Lists.newArrayList(scopes()))
                .build();

        return oauth;
    }

    @Bean
    public SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(securityReferences())
                .forPaths(Predicates.alwaysTrue())
                .build();
    }
}
```

## API configuration

Defnining API groups separated on technical areas:

```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Autowired
    private SecurityScheme securityScheme;

    @Autowired
    private SecurityContext securityContext;

    private final Predicate<String> COMPANY_API = PathSelectors.ant("/secured/company/**");
    private final Predicate<String> OAUTH_API = PathSelectors.ant("/oauth/**");

    @Bean
    public Docket companyApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("Company API")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(COMPANY_API)
                .build()
                .securitySchemes(Lists.newArrayList(securityScheme))
                .securityContexts(Lists.newArrayList(securityContext));
    }

    @Bean
    public Docket authenticationApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("OAuth 2.0 API")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(OAUTH_API)
                .build();
    }

    private ApiInfo apiInfo() {
        Contact contact = new Contact("Adam Zaręba", "http://adamzareba.github.io", "testEmail@gmail.com");
        return new ApiInfoBuilder()
                .title("Company Application REST API")
                .description("List of available API served by Company Application")
                .version("1.0")
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .contact(contact)
                .build();
    }
}
```

## Controllers

Documenting REST controllers:

```java
@RestController
@RequestMapping("/secured/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @ApiOperation(value = "Find all companies", notes = "Returns all available companies in the system")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Not authenticated"),
            @ApiResponse(code = 403, message = "Not authorized to see companies"),
            @ApiResponse(code = 404, message = "Companies not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public @ResponseBody
    List<Company> getAll() {
        return companyService.getAll();
    }

    @ApiOperation(value = "Find company by id", notes = "Id is required")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Not authenticated"),
            @ApiResponse(code = 403, message = "Not authorized to see company"),
            @ApiResponse(code = 404, message = "Company not found with given id"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public @ResponseBody
    Company get(@PathVariable Long id) {
        return companyService.get(id);
    }

    @ApiOperation(value = "Find company by name", notes = "Name is required")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Not authenticated"),
            @ApiResponse(code = 403, message = "Not authorized to see company"),
            @ApiResponse(code = 404, message = "Company not found with given name"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(value = "/filter", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public @ResponseBody
    Company get(@RequestParam String name) {
        return companyService.get(name);
    }

    @ApiOperation(value = "Create company", notes = "Id is not required (it will be filled automatically)")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Company created successfully", response = void.class),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Not authenticated"),
            @ApiResponse(code = 403, message = "Not authorized to see companies"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    public ResponseEntity<?> create(@RequestBody Company company) {
        companyService.create(company);
        HttpHeaders headers = new HttpHeaders();
        ControllerLinkBuilder linkBuilder = linkTo(methodOn(CompanyController.class).get(company.getId()));
        headers.setLocation(linkBuilder.toUri());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Update company", notes = "Id is required")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 201, message = "Company updated successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Not authenticated"),
            @ApiResponse(code = 403, message = "Not authorized to see companies"),
            @ApiResponse(code = 404, message = "Company not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void update(@RequestBody Company company) {
        companyService.update(company);
    }

    @ApiOperation(value = "Delete company", notes = "Id is required")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "Company deleted successfully"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Not authenticated"),
            @ApiResponse(code = 403, message = "Not authorized to see companies"),
            @ApiResponse(code = 404, message = "Company not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }
}
```

## Models

Documenting transfer models:

```java
@Entity
@Table(name = "COMPANY", uniqueConstraints = {@UniqueConstraint(columnNames = {"NAME"})})
@Getter
@Setter
@ApiModel
@EqualsAndHashCode(of = "id")
public class Company implements Serializable {

    @ApiModelProperty(value = "The database generated company id", required = true, readOnly = true)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id = null;

    @ApiModelProperty(value = "Company name", required = true, example = "Pepsi")
    @Column(name = "NAME", nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private Set<Department> departments = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private Set<Car> cars = new HashSet<>();

    public void setDepartments(Set<Department> departments) {
        this.departments.clear();
        if (departments != null) {
            this.departments.addAll(departments);
        }
    }

    public void setCars(Set<Car> cars) {
        this.cars.clear();
        if (cars != null) {
            this.cars.addAll(cars);
        }
    }
}
```