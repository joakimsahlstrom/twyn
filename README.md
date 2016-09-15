# twyn!
Twyn maps json to java, using jackson under the hood. It allows for lenient parsing with little code.

Requires Java 8. RAD?

##Features:
###Twyn only requires interfaces for mapping
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

###Twyn can fall back on jackson:s json->java mapping
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

###Twyn support JavaBeans style naming of properties
The json field "xxx" can be mapped by getXxx, hasXxx or xxx(). 

###Twyn supports arrays and collections
```json
{
	"daughters" : [ { "name" : "Inara" }, { "name" : "Kaylee" }, { "name" : "River" } ],
	"daughterNickNames" : {
		"Inara" : { "nick" : "innie" },
		"Kaylee" : { "nick" : "lee" }
	},
	"sons" : [ "Mal", "Wash" ],
	"unknowns" : [ { "name" : "Chtulu", "type" : "squid" }, { "name" : "Donald", "type" : "duck" } ],
	"songs" : [ { "name" : "Come out and play" }, { "name" : "LAPD" } ]
}
```
Can be mapped with:
```java
interface Offspring {
	Daughter[] daughters();
	
	@TwynCollection(Nick.class) // Collections without this annotation are ignored
	Map<String, Nick> daughterNickNames();
	
	@TwynCollection(value=Nick.class, keyType=DaughterKey.class)
	Map<DaughterKey, Nick> getDaughterNickNames(); // Typed key must have constructor(String)
	
	String[] sons();
	
	@TwynCollection(Entity.class) // ...goes for all collection types
	List<Entity> getUnknowns();
	
	@TwynCollection(Song.class)
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

###Twyn can map interfaces directly against arrays
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
	@TwynIndex(0) int index(); // @TwynIndex must exist on all getter methods
	@TwynIndex(3) String message();
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


###Twyn can be configured for different use cases
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

###Twyn supports annotation processing for proxy classes
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

###Twyn can process collections in parallel
```java
interface Offspring {
	// ...
	@TwynCollection(value = Nick.class, parallel = true)
	Map<String, Nick> daughterNickNames();
	// ...
}
```
Likely only efficient for large collections

###Twyn supports toString, hashCode and equals
Equals and hashCode are calculated from all mapped values, or, if any, those annotated with @TwynId.
toString prints the values that equals and hashCode are calculated from.

###Twyn can modify the underlying jackson node structure
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
Retrieve the underlying jackson jsonNode like this:
```java
Twyn twyn = Twyn.forTest();
Contact contact = twyn.read(jsonResponse, Contact.class);
// modify contact...
JsonNode root = twyn.getJsonNode(contact);
```

##Todo:
* Improve error message for bad array mappings
* @PostConstruct annotated methods
* Have Optional as a return type to allow testing of values / has-methods that can test if an underlying node is present?
* Ability to parse json that starts with a Map (eg. { \"a\" : { 1 }, \"b\" : { 2 } } w/ 
	interface MyMap { @TwynCollection(MyNode.class) @TwynRoot Map<String, MyNode> nodes(); }
	twyn.read(jsonResponse, MyMap.class)
	or
	twyn.readMap(jsonResponse, MyNode.class);
* Delete values/structures
* Collection modifications
* Ability to chose custom names for fields by using annotation (e.g. @TwynName)
* Typed map should check keytype in annotation against return value of method (can it?)
