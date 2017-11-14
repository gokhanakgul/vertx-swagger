package com.github.phiz71.vertx.swagger.codegen;

import io.swagger.codegen.*;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.models.*;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.swagger.models.parameters.Parameter;
public class JavaVertXServerGenerator extends AbstractJavaCodegen {

    private static final String ROOT_PACKAGE = "io.swagger.server.api";

    //CliOptions
    private static final String API_IMPL_GENERATION_OPTION = "apiImplGeneration";
    private static final String JSON_OBJECT_MODEL_GENERATION_OPTION = "jsonObjectModelGeneration";
    public static final String JDBC_PERSISTENCE = "jdbcPersistence";
    public static final String JDBC_TABLE_PREFIX = "jdbcTablePrefix";
    public static final String JDBC_DEFAULT_TABLE_PREFIX = "tbl_";
    public static final String JDBC_IDENTITY_FIELD = "jdbcIdField";
    public static final String JDBC_DEFAULT_IDENTITY_FIELD = "id";

    private static final String MAIN_API_VERTICAL_GENERATION_OPTION = "mainVerticleGeneration";
    private static final String RX_INTERFACE_OPTION = "rxInterface";
    private static final String VERTX_SWAGGER_ROUTER_VERSION = "vertxSwaggerRouterVersion";

    //vendorExtensionsKeys
    private static final String VENDOR_EXTENSIONS_IS_JSONIFIABLE = "X-isJsonifiable";
    private static final String VENDOR_EXTENSIONS_IS_UUID = "X-isUUID";
    private static final String VENDOR_EXTENSIONS_JSON_GETTER = "X-JSONGetter";
    private static final String VENDOR_EXTENSIONS_NEED_CAST = "X-needCast";
    private static final String VENDOR_EXTENSIONS_UPPER_SNAKE_CASE = "X-UPPER_SNAKE_CASE";

    //Specific Java types & dataTypes
    private static final String DATATYPE_ARRAY = "array";
    private static final String DATATYPE_MAP = "map";
    private static final String TYPE_INSTANT = "Instant";
    private static final String TYPE_JSON_ARRAY = "JsonArray";
    private static final String TYPE_JSON_OBJECT = "JsonObject";
    private static final String TYPE_JSON_INCLUDE = "JsonInclude";
    private static final String TYPE_JSON_PROPERTY = "JsonProperty";
    private static final String TYPE_JSON_VALUE = "JsonValue";

    private String resourceFolder = "src/main/resources";
    private String apiVersion = "1.0.0-SNAPSHOT";
    private boolean isJsonObjectGeneration = false;

    public JavaVertXServerGenerator() {
        super();

        reservedWords.add("user");

        this.setDateLibrary("java8");

        // set the output folder here
        outputFolder = "generated-code/javaVertXServer";

        /*
         * Models. You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here. for
         * multiple files for model, just put another entry in the
         * `modelTemplateFiles` with a different extension
         */
        modelTemplateFiles.clear();

        modelTemplateFiles.put("model.mustache", // the template to use
                ".java"); // the extension for each file to write

        /*
         * Api classes. You can write classes for each Api file with the
         * apiTemplateFiles map. as with models, add multiple entries with
         * different extensions for multiple files per class
         */
        apiTemplateFiles.clear();
        apiTemplateFiles.put("api.mustache", // the template to use
                ".java"); // the extension for each file to write

        apiTemplateFiles.put("apiImpl.mustache", // the template to use
                "Impl.java"); // the extension for each file to write

        apiTemplateFiles.put("apiVerticle.mustache", // the template to use
                "Verticle.java"); // the extension for each file to write
        apiTemplateFiles.put("apiException.mustache", // the template to use
            "Exception.java"); // the extension for each file to write
        apiTemplateFiles.put("apiHeader.mustache", // the template to use
            "Header.java"); // the extension for each file to write

        /*
         * Template Location. This is the location which templates will be read
         * from. The generator will use the resource stream to attempt to read
         * the templates.
         */
        embeddedTemplateDir = templateDir = "javaVertXServer";

        /*
         * Api Package. Optional, if needed, this can be used in templates
         */
        apiPackage = ROOT_PACKAGE + ".verticle";

        /*
         * Model Package. Optional, if needed, this can be used in templates
         */
        modelPackage = ROOT_PACKAGE + ".model";


        testPackage = ROOT_PACKAGE + ".verticle";

        /*
         * Invoker Package. Optional, if needed, this can be used in templates
         */
        invokerPackage = ROOT_PACKAGE;

        groupId = "io.swagger";
        artifactId = "swagger-java-vertx-server";
        artifactVersion = apiVersion;

        String vertxSwaggerRouterVersion = ResourceBundle.getBundle("vertx-swagger-router").getString("vertx-swagger-router.version");
        additionalProperties.put(VERTX_SWAGGER_ROUTER_VERSION, vertxSwaggerRouterVersion);

        cliOptions.add(CliOption.newBoolean(RX_INTERFACE_OPTION,
            "When specified, API interfaces are generated with RX and methods return Single<>."));

        cliOptions.add(CliOption.newBoolean(MAIN_API_VERTICAL_GENERATION_OPTION,
            "When specified, MainApiVerticle.java will not be generated"));

        cliOptions.add(CliOption.newBoolean(JDBC_PERSISTENCE,
                "When specified, JDBC feature will be enabled"));
        cliOptions.add(CliOption.newBoolean(API_IMPL_GENERATION_OPTION,
            "When specified, xxxApiImpl.java will be generated"));

        cliOptions.add(CliOption.newBoolean(JSON_OBJECT_MODEL_GENERATION_OPTION,
            "When specified, model classes will extend JsonObject"));
    }

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /**
     * Configures a friendly name for the generator. This will be used by the generator to select
     * the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    public String getName() {
        return "java-vertx";
    }

    /**
     * Returns human-friendly help for the generator. Provide the consumer with help tips,
     * parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        return "Generates a java-Vert.X Server library.";
    }


    @Override
    public void processOpts() {
        super.processOpts();

//        apiTestTemplateFiles.clear();
        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();

        importMapping.remove("JsonCreator");
        importMapping.remove("com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put(TYPE_JSON_INCLUDE, "com.fasterxml.jackson.annotation.JsonInclude");
        importMapping.put(TYPE_JSON_PROPERTY, "com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put(TYPE_JSON_VALUE, "com.fasterxml.jackson.annotation.JsonValue");
        importMapping.put("MainApiException", invokerPackage + ".MainApiException");
        importMapping.put("MainApiHeader", invokerPackage + ".MainApiHeader");
        importMapping.put("ResourceResponse", invokerPackage + ".util.ResourceResponse");
        importMapping.put("VerticleHelper", invokerPackage + ".util.VerticleHelper");

        supportingFiles.clear();
        supportingFiles.add(new SupportingFile("swagger.mustache", resourceFolder, "swagger.json"));

        Object mainApiVerticleGenerationOption = additionalProperties.get(MAIN_API_VERTICAL_GENERATION_OPTION);
        if (mainApiVerticleGenerationOption == null || Boolean.parseBoolean(mainApiVerticleGenerationOption.toString())) {
            supportingFiles.add(new SupportingFile("MainApiVerticle.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator), "MainApiVerticle.java"));
        }
        Object apiImplGenerationOption = additionalProperties.get(API_IMPL_GENERATION_OPTION);
        if (apiImplGenerationOption != null && Boolean.parseBoolean(apiImplGenerationOption.toString())) {
            apiTemplateFiles.put("apiImpl.mustache", // the template to use
                "Impl.java"); // the extension for each file to write
        }

        Object jsonObjectModelGenerationOption = additionalProperties.get(JSON_OBJECT_MODEL_GENERATION_OPTION);
        this.isJsonObjectGeneration = (jsonObjectModelGenerationOption != null && Boolean.parseBoolean(jsonObjectModelGenerationOption.toString()));
        String apiTemplate = "";
        String apiVerticleTemplate = "";
        String modelTemplate = "";
        if (this.isJsonObjectGeneration) {
            apiTemplate = "json/apiJson.mustache";
            apiVerticleTemplate = "json/apiVerticleJson.mustache";
            modelTemplate = "json/modelJson.mustache";

            supportingFiles.add(new SupportingFile("json/VerticleHelperJson.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator) + File.separator + "util", "VerticleHelper.java"));

            typeMapping.put("date", TYPE_INSTANT);
            typeMapping.put("DateTime", TYPE_INSTANT);
            importMapping.put(TYPE_INSTANT, "java.time.Instant");

            typeMapping.put(DATATYPE_ARRAY, TYPE_JSON_ARRAY);
            typeMapping.put(DATATYPE_MAP, TYPE_JSON_OBJECT);
            instantiationTypes.put(DATATYPE_ARRAY, TYPE_JSON_ARRAY);
            instantiationTypes.put(DATATYPE_MAP, TYPE_JSON_OBJECT);
            importMapping.put(TYPE_JSON_OBJECT, "io.vertx.core.json.JsonObject");
            importMapping.put(TYPE_JSON_ARRAY, "io.vertx.core.json.JsonArray");

            typeMapping.put("UUID", "String");
        } else {
            apiTemplate = "api.mustache";
            apiVerticleTemplate = "apiVerticle.mustache";
            modelTemplate = "model.mustache";
            supportingFiles.add(new SupportingFile("VerticleHelper.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator) + File.separator + "util", "VerticleHelper.java"));

        }
        apiTemplateFiles.put(apiTemplate, // the template to use
            ".java"); // the extension for each file to write

        apiTemplateFiles.put(apiVerticleTemplate, // the template to use
            "Verticle.java"); // the extension for each file to write

        modelTemplateFiles.put(modelTemplate, // the template to use
            ".java"); // the extension for each file to write


        supportingFiles.add(new SupportingFile("MainApiException.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator), "MainApiException.java"));
        supportingFiles.add(new SupportingFile("JsonHelper.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator), "JsonHelper.java"));

        supportingFiles.add(new SupportingFile("MainApiHeader.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator), "MainApiHeader.java"));
        supportingFiles.add(new SupportingFile("ResourceResponse.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator) + File.separator + "util", "ResourceResponse.java"));

        writeOptional(outputFolder, new SupportingFile("SwaggerManager.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator) + File.separator + "util", "SwaggerManager.java"));
        writeOptional(outputFolder, new SupportingFile("vertx-default-jul-logging.mustache", resourceFolder, "vertx-default-jul-logging.properties"));
        writeOptional(outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
        writeOptional(outputFolder, new SupportingFile("README.mustache", "", "README.md"));
        writeOptional(outputFolder, new SupportingFile("executer-batch.mustache", "", "run-with-config.sh"));
        writeOptional(outputFolder, new SupportingFile("vertx-application-config.mustache", "", "config.json"));
        writeOptional(outputFolder, new SupportingFile("swagger-codegen-ignore.mustache", "", ".swagger-codegen-ignore"));

        if (Boolean.parseBoolean(additionalProperties.getOrDefault(JDBC_PERSISTENCE, "false").toString())) {
            supportingFiles.add(new SupportingFile("JDBCVerticle.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator), "JDBCVerticle.java"));
            supportingFiles.add(new SupportingFile("SQLHelper.mustache", sourceFolder + File.separator + invokerPackage.replace(".", File.separator), "SQLHelper.java"));
            supportingFiles.add(new SupportingFile("dbInitialScript.mustache", resourceFolder, "dbInit.sql"));
            supportingFiles.add(new SupportingFile("dbTestInitialScript.mustache", resourceFolder, "dbInitHSQL.sql"));
            writeOptional(outputFolder, new SupportingFile("hikari.mustache", resourceFolder, "hikari.properties"));
            writeOptional(outputFolder, new SupportingFile("c3p0.mustache", resourceFolder, "c3p0.properties"));
        }

    }


    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        if (this.isJsonObjectGeneration) {
            manageJsonCapability(property);
            property.vendorExtensions.put(VENDOR_EXTENSIONS_UPPER_SNAKE_CASE, camelCaseToUpperSnakeCase(property.nameInCamelCase));
        } else {
            if (serializeBigDecimalAsString && property.baseType.equals("BigDecimal")) {
                // we serialize BigDecimal as `string` to avoid precision loss
                property.vendorExtensions.put("extraAnnotation", "@JsonSerialize(using = ToStringSerializer.class)");

                // this requires some more imports to be added for this model...
                model.imports.add("ToStringSerializer");
                model.imports.add("JsonSerialize");
            }

            if (DATATYPE_ARRAY.equals(property.containerType)) {
                model.imports.add("ArrayList");
            } else if ("map".equals(property.containerType)) {
                model.imports.add("HashMap");
            }

        }


    }

    private void manageJsonCapability(CodegenProperty property) {
        //by default all is Jsonifiable and no parameter need to be casted.
        property.vendorExtensions.put(VENDOR_EXTENSIONS_NEED_CAST, false);
        property.vendorExtensions.put(VENDOR_EXTENSIONS_IS_JSONIFIABLE, true);

        if (property.isListContainer) {
            addJsonGetterVendorExtensions(property, "getJsonArray");
        } else if (property.isMapContainer) {
            addJsonGetterVendorExtensions(property, "getJsonObject");
        } else if (property.isDate || property.isDateTime) {
            addJsonGetterVendorExtensions(property, "getInstant");
        } else if (property.isString) {
            addJsonGetterVendorExtensions(property, "getString");
        } else if (property.isInteger) {
            addJsonGetterVendorExtensions(property, "getInteger");
        } else if (property.isLong) {
            addJsonGetterVendorExtensions(property, "getLong");
        } else if (property.isFloat) {
            addJsonGetterVendorExtensions(property, "getFloat");
        } else if (property.isDouble) {
            addJsonGetterVendorExtensions(property, "getDouble");
        } else if (property.isByteArray) {
            addJsonGetterVendorExtensions(property, "getBinary");
        } else if (property.isBinary) {
            addJsonGetterVendorExtensions(property, "getBinary");
        } else if (property.isBoolean) {
            addJsonGetterVendorExtensions(property, "getBoolean");
        } else if (property.jsonSchema.contains("$ref")) {
            //is a JsonObject
            addJsonGetterVendorExtensions(property, "getJsonObject");
            property.vendorExtensions.put(VENDOR_EXTENSIONS_NEED_CAST, true);
        } else {
            property.vendorExtensions.put(VENDOR_EXTENSIONS_IS_JSONIFIABLE, false);
            addJsonGetterVendorExtensions(property, "getString");
        }
    }

    private void addJsonGetterVendorExtensions(CodegenProperty property, String jsonGetter) {
        property.vendorExtensions.put(VENDOR_EXTENSIONS_JSON_GETTER, jsonGetter);
    }

    private String camelCaseToUpperSnakeCase(String camelCaseString) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return (camelCaseString.replaceAll(regex, replacement).toUpperCase());
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (this.isJsonObjectGeneration) {
            String swaggerType = getSwaggerType(p);
            if (typeMapping.containsKey(swaggerType)) {
                return typeMapping.get(swaggerType);
            }
            return swaggerType;
        } else {
            return super.getTypeDeclaration(p);
        }
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        if ("UUID".equals(parameter.dataType)) {
            parameter.vendorExtensions.put(VENDOR_EXTENSIONS_IS_UUID, true);
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> newObjs = super.postProcessOperations(objs);
        Map<String, Object> operations = (Map<String, Object>) newObjs.get("operations");
        assert operations != null;
        List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation operation : ops) {
            operation.httpMethod = operation.httpMethod.toLowerCase();

            if (operation.getHasPathParams()) {
                operation.path = camelizePath(operation.path);
            }

            for (CodegenParameter param : operation.allParams) {
                if ("UUID".equals(param.dataType)) {
                    param.vendorExtensions.put("X-isUUID", true);
                }
            }
        }
        return newObjs;
    }


    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, definitions, swagger);
        codegenOperation.imports.add("MainApiException");
        codegenOperation.imports.add("MainApiHeader");
        codegenOperation.imports.add("ResourceResponse");
        codegenOperation.imports.add("VerticleHelper");

        if (this.isJsonObjectGeneration) {
            codegenOperation.imports.add(TYPE_JSON_OBJECT);
            codegenOperation.imports.add(TYPE_JSON_ARRAY);
        }

        for (Map.Entry<String, Response> entry : operation.getResponses().entrySet()) {
            Response response = entry.getValue();
            CodegenResponse r = fromResponse(entry.getKey(), response);

            for (CodegenProperty header : r.headers) {
                if (header.baseType != null &&
                    !defaultIncludes.contains(header.baseType) &&
                    !languageSpecificPrimitives.contains(header.baseType)) {
                    codegenOperation.imports.add(header.complexType);
                }
            }
        }

        return codegenOperation;
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        codegenModel.imports.remove("ApiModel");
        //codegenModel.imports.remove("ApiModelProperty");
        codegenModel.allVars.stream().forEach(codegenProperty  -> {
            if (JDBC_DEFAULT_IDENTITY_FIELD.equals(codegenProperty.name))
                codegenProperty.vendorExtensions.put("identity",true);
        });
        if (!this.isJsonObjectGeneration) {
            if (codegenModel.isEnum || codegenModel.hasEnums) {
                codegenModel.imports.add(TYPE_JSON_VALUE);
            }
            if (!codegenModel.isEnum) {
                codegenModel.imports.add(TYPE_JSON_INCLUDE);
                codegenModel.imports.add(TYPE_JSON_PROPERTY);
                codegenModel.imports.add("Objects");
            }
        } else {
            if (!codegenModel.isEnum) {
                codegenModel.imports.add(TYPE_JSON_INCLUDE);
                codegenModel.imports.add(TYPE_JSON_PROPERTY);
                codegenModel.imports.add(TYPE_JSON_OBJECT);
            }

        }
        return codegenModel;

    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);

        // add full swagger definition in a mustache parameter
        String swaggerDef = Json.pretty(swagger);
        this.additionalProperties.put("fullSwagger", swaggerDef);

        // add server port from the swagger file, 8080 by default
        String host = swagger.getHost();
        String port = extractPortFromHost(host);
        this.additionalProperties.put("serverPort", port);

        // retrieve api version from swagger file, 1.0.0-SNAPSHOT by default
        if (swagger.getInfo() != null && swagger.getInfo().getVersion() != null)
            artifactVersion = apiVersion = swagger.getInfo().getVersion();
        else
            artifactVersion = apiVersion;

        if (Boolean.parseBoolean(additionalProperties.getOrDefault(JDBC_PERSISTENCE, "false").toString())) {
            this.additionalProperties.put(JDBC_TABLE_PREFIX, JDBC_DEFAULT_TABLE_PREFIX);
        }

        // manage operation & custom serviceId
        Map<String, Path> paths = swagger.getPaths();
        if (paths != null) {
            for (Entry<String, Path> entry : paths.entrySet()) {
                manageOperationNames(entry.getValue(), entry.getKey(), swagger.getDefinitions());
            }
        }
    }

    private void manageOperationNames(Path path, String pathname, Map<String, Model> definitions) {
        String serviceIdTemp;

        Map<HttpMethod, Operation> operationMap = path.getOperationMap();
        if (operationMap != null) {
            for (Entry<HttpMethod, Operation> entry : operationMap.entrySet()) {
                serviceIdTemp = computeServiceId(pathname, entry);
                entry.getValue().setVendorExtension("x-serviceId", serviceIdTemp);
                entry.getValue().setVendorExtension("x-serviceIduC", serviceIdTemp.toUpperCase());
                entry.getValue().setVendorExtension("x-serviceId-varName", serviceIdTemp.toUpperCase() + "_SERVICE_ID");
                entry.getValue().setVendorExtension("x-isPost",entry.getKey().equals(HttpMethod.POST));
                entry.getValue().setVendorExtension("x-isGet",entry.getKey().equals(HttpMethod.GET));
                entry.getValue().setVendorExtension("x-isPut",entry.getKey().equals(HttpMethod.PUT));
                entry.getValue().setVendorExtension("x-isPatch",entry.getKey().equals(HttpMethod.PATCH));
                entry.getValue().setVendorExtension("x-isHead",entry.getKey().equals(HttpMethod.HEAD));
                entry.getValue().setVendorExtension("x-isDelete",entry.getKey().equals(HttpMethod.DELETE));
                prepareSQLs(entry, definitions);
            }
        }
    }

    private void prepareSQLs(Entry<HttpMethod, Operation> entry, Map<String, Model> definitions) {
        if (Boolean.parseBoolean(additionalProperties.getOrDefault(JDBC_PERSISTENCE, "false").toString())) {

            StringBuilder sqlQuery = new StringBuilder();

            String tableName = JDBC_DEFAULT_TABLE_PREFIX + "TABLE_NAME";
            String whereClause = "";
            //String queryMethod = "";
            switch (entry.getKey()) {
                case GET:
                    sqlQuery.append("SELECT * FROM ").append(tableName);

                    whereClause = extractWhereClause(entry);
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(whereClause)) {
                        sqlQuery.append(" WHERE ").append(whereClause);
                    }

                    break;

                case POST:
                    sqlQuery.append("INSERT INTO ").append(tableName);

                    whereClause = extractWhereClause(entry);
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(whereClause)) {
                        sqlQuery.append(" WHERE ").append(whereClause);
                    }

                    break;

                case PUT:
                    sqlQuery.append("UPDATE ").append(tableName).append(" SET ");

                    whereClause = extractWhereClause(entry);
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(whereClause)) {
                        sqlQuery.append(" WHERE ").append(whereClause);
                    }

                    break;
                case PATCH:

                    sqlQuery.append("UPDATE ").append(tableName).append(" SET ");


                    whereClause = extractWhereClause(entry);
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(whereClause)) {
                        sqlQuery.append(" WHERE ").append(whereClause);
                    }

                    break;
                case DELETE:
                    sqlQuery.append("DELETE FROM ").append(tableName);

                    whereClause = extractWhereClause(entry);
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(whereClause)) {
                        sqlQuery.append(" WHERE ").append(whereClause);
                    }

                    break;
            }

            entry.getValue().setVendorExtension("x-serviceId-SQL", sqlQuery);

        }
    }

    private String extractWhereClause(Entry<HttpMethod, Operation> entry) {
        StringBuilder sqlQuery = new StringBuilder();
        boolean addAnd = false;
        for (Parameter param : entry.getValue().getParameters()) {
            if (param.getIn().equals("path") || param.getIn().equals("query")) {
                if (addAnd) {
                    sqlQuery.append(" AND ");
                }
                sqlQuery.append(param.getName() + " ").append('=').append(" ?");
                addAnd = true;
            }
        }
        return sqlQuery.toString();
    }


    private String extractValuesData(Entry<HttpMethod, Operation> entry, Map<String, Model> definitions, String tableName) {
        StringBuilder sqlQuery = new StringBuilder(" VALUES ( ");
        boolean hasFormData = false;
        boolean hasBody = false;


        for (Parameter param : entry.getValue().getParameters()) {
            if (param.getIn().equals("formData")) {
                if (hasFormData)
                    sqlQuery.append(",");
                sqlQuery.append(" ? ");
                hasFormData = true;
            }
        }


        for (Parameter param : entry.getValue().getParameters()) {
            if (param.getIn().equals("body")) {
                hasBody = true;
                //definitions.get(tableName).getProperties()
                int count = 0;
                while (count < definitions.get(tableName).getProperties().size()) {
                    if (count > 0)
                        sqlQuery.append(",");
                    sqlQuery.append(" ? ");
                    count++;
                }
            }
        }

        sqlQuery.append(")");

        return sqlQuery.toString();
    }


    //private String

    private String computeServiceId(String pathname, Entry<HttpMethod, Operation> entry) {
        String operationId = entry.getValue().getOperationId();
        return (operationId != null) ? operationId : entry.getKey().name() + pathname.replaceAll("-", "_").replaceAll("/", "_").replaceAll("[{}]", "");
    }

    protected String extractPortFromHost(String host) {
        if (host != null) {
            int portSeparatorIndex = host.indexOf(':');
            if (portSeparatorIndex >= 0 && portSeparatorIndex + 1 < host.length()) {
                return host.substring(portSeparatorIndex + 1);
            }
        }
        return "8080";
    }

    private String camelizePath(String path) {
        String word = path;
        Pattern p = Pattern.compile("\\{([^/]*)\\}");
        Matcher m = p.matcher(word);
        while (m.find()) {
            word = m.replaceFirst(":" + m.group(1));
            m = p.matcher(word);
        }
        p = Pattern.compile("(_)(.)");
        m = p.matcher(word);
        while (m.find()) {
            word = m.replaceFirst(m.group(2).toUpperCase());
            m = p.matcher(word);
        }
        return word;
    }
}
