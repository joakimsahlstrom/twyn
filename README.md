# twyn!
Twyn maps json to java, using jackson under the hood. It allows for lenient parsing with little code.

Requires Java 11. Lets you follow the [Robustness principle](https://en.wikipedia.org/wiki/Robustness_principle).

[![Build Status](https://travis-ci.org/joakimsahlstrom/twyn.svg?branch=master)](https://travis-ci.org/joakimsahlstrom/twyn)

## Features:
### Twyn only requires interfaces for mapping
Given this json where only "firstname" and "country" is interesting...
```json
{
	"firstname" : "John",
	"lastname" : "Doe",
	"age" : "38",
	"address" : {
		"street" : "Street 1",
		"city" : "London",
		"country" : "England"
	}
}
```
...define these interfaces and parse like below:
```java
interface Contact {
	String getFirstname();
	AddressInfo getAddress();
}
interface AddressInfo {
	String getCountry();
}

public void doStuff(InputStream jsonResponse) {
	Contact contact = Twyn.forTest().read(jsonResponse, Contact.class);
	String firstName = contact.getFirstname();
	String country = contact.getAddress().getCountry();
	System.out.println(firstName + " lives in " + country); // outputs "John lives in England"
}
```

### Twyn supports default methods
```java
interface Contact {
	String getName();
	default String greet() {
		return "Hello " + getName() + "!"; 
	}
}

public void doStuff() {
	String json = "{ \"name\" : \"Donny\" }";
	System.out.println(new Twyn().read(json, Contact.class).greet()); // outputs "Hello Donny!" 
}
```

### Twyn supports Optional<>
```java
interface Contact {
	Optional<String> getNickName();
	Optional<Address> getAlternativeAddess();
}

public void doStuff() {
	String json = "{ \"nickName\" : \"nick\" }";
	System.out.println(new Twyn().read(json, Contact.class).getAlternativeAddress().isPresent()); // outputs false
	System.out.println(new Twyn().read(json, Contact.class).getNickName().isPresent()); // outputs true
}
```

### Twyn can fall back on jackson:s json->java mapping
```java
interface Contact {
	String getFirstname();
	Address getAddress();
}
class Address {
	private String street;
	private String city;
	private String country;
	public String getStreet() {
		return street;
	}
	public String getCity() {
		return city;
	}
	public String getCountry() {
		return country;
	}
}

public void doStuff(InputStream jsonResponse) {
	// Same json as first example
	Contact contact = Twyn.forTest().read(jsonResponse, Contact.class);
	Address address = contact.getAdress(); // This will return an Address instance by using the jackson java object mapper
}
```

### Twyn support JavaBeans style naming of properties
The json field "xxx" can be mapped by getXxx, hasXxx or xxx(). 

### Twyn supports arrays and collections
```json
{
	"daughters" : [ { "name" : "Amy" }, { "name" : "Bonnie" }, { "name" : "Carol" } ],
	"daughterNickNames" : {
		"Amy" : { "nick" : "a" },
		"Bonnie" : { "nick" : "B" }
	},
	"sons" : [ "Al", "Ben" ],
	"unknowns" : [ { "name" : "Chtulu", "type" : "squid" }, { "name" : "Donald", "type" : "duck" } ],
	"songs" : [ { "name" : "A song" }, { "name" : "Another song" } ]
}
```
Can be mapped with:
```java
interface Offspring {
	Daughter[] daughters();
	
	Map<String, Nick> daughterNickNames();
	Map<DaughterKey, Nick> getDaughterNickNames(); // Typed key must have constructor(String)
	
	String[] sons();
	
	List<Entity> getUnknowns();
	
	Set<Song> songs(); // Object equality is described below
}
interface Daughter {
	String getName();
}
interface Nick {
	String nick();
}
interface Entity {
	String name();
	String type();
}
interface Song {
	String name();
}
class DaughterKey {
	public DaughterKey(String key) {
		// ...
	}
	// ...
}
```

#### Twyn can perform a trick to read a root Map structure
```json
{
	"a": { "s": "str", "i": 2 },
	"b": { "s": "str2", "i": 3 }
}
```
can be mapped with
```java
public interface MapValue {
	String s();
	int i();
}
public interface MapValueMap extends Map<String, MapValue> { }
// Read by using this little trick, note that Twyn will not return a MapValueMap class here
Map<String, MapValue> = twyn.read(data, MapValueMap.class);
```


### Twyn can map nodes deeper in a json document
```json
{
    "name": {
        "firstName": "John",
        "lastName": "Doe"
    },
    "age": 37
}
```
Can be mapped with
```java
interface Person {
    @Resolve("name.firstName") String getFirstName();
    int getAge();
}
```

### Twyn can map interfaces directly to json arrays
Map this json:
```json
{ 
	"arr" : [ 1, "JS", 33, "iCode" ] 
}
```
with this interface:
```java
public static interface ArrayObject {
	ArrayElement arr();
}
public static interface ArrayElement {
	@ArrayIndex(0) int index(); // @ArrayIndex must exist on all get/set methods
	@ArrayIndex(3) String message();
	@ArrayIndex(3) void setMessage(String msg);
}
```
__Also, this way the two-dimensional array:__
```json
{ 
	"arr" : [ 
		[ 1, "JS", 33, "iCode" ],
		[ 2, "LS", 30, "iChem" ]
	] 
}

```
can be mapped with this interface:
```java
public static interface ArrayObject {
	ArrayElement[] arr();
}
```
__If a json structure starts with an array:__
```json
[ 
	[ 1, "JS", 33, "iCode" ],
	[ 2, "LS", 30, "iChem" ]
] 
```
it can be parsed like this:
```java
ArrayElement[] elements = twyn.read(jsonResponse, ArrayElement[].class);
```

### Twyn supports interface inheritance
Instances of C1 in the example below will have all properties specified by Parent1 & Parent2 (duh!)
```java
public interface C1 extends Parent1, Parent2 {
	String getName();
}
public interface Parent1 {
	String getLastName();
}
public interface Parent2 {
	String getMiddleName();
}
```


### Twyn can be configured for different use cases
```java
// Use either java proxies or runtime-generated classes
Twyn.configurer().withClassGeneration().configure(); // faster over time
Twyn.configurer().withJavaProxies().configure(); // faster startup
// Alternate ObjectMappers can be used
Twyn.configurer().withClassGeneration().withObjectMapper(myObjectMapper).configure();
// Caching can be used if values will be retrieved several times
Twyn.configurer().withClassGeneration().withFullCaching().configure();
// It is possible to precompile classes to avoid temporary runtime slowdowns
Twyn.configurer().withClassGeneration().withPrecompiledClasses(myClasses).configure();
// debugMode gives proxies more detailed toString output
Twyn.configurer().withClassGeneration().withDebugMode().configure();
```

### Twyn supports annotation processing for proxy classes
```java
@TwynProxy
public static interface ArrayObject {
	ArrayElement arr();
	
	// Inner interfaces are automatically handled if 
	// containing class is annotated with @TwynProxy
	public interface Inner {
		String getName();
	}
}
```
A twyn proxy for this class will generated directly at compile-time. Use the configuration .withClassGeneration() in order to use the generated class(es). 

### Twyn supports toString, hashCode and equals
Equals and hashCode are calculated from all mapped values, or, if any, those annotated with @IdField.
toString prints the values that equals and hashCode are calculated from.

### Twyn can modify the underlying jackson node structure
Setters returns _this_ if return type equals interface type
```java
interface Setter {
	void setName(String n); // modifies the field "name"
	Setter visible(boolean vsbl); // modifies the field "visible", returns this
	void setId(Id id); // "Id" must be mappable by jackson
}
class Id {
	int idValue;
}
```
Setters works with @ArrayIndex(N) & @Resolve("...")

Retrieve the underlying jackson jsonNode like this:
```java
Twyn twyn = Twyn.forTest();
Contact contact = twyn.read(jsonResponse, Contact.class);
// modify contact...
JsonNode root = twyn.getJsonNode(contact);
```

## Todo:
* @PostConstruct annotated methods
* Improve error message for bad array mappings
* Ability to parse [ { "field": "val" }, { ... }, ... ] with something like interface Fields { @Resolve("/") List<FieldHolder> ...
* Delete values/structures
* Collection modifications
