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
	Map<String, Nick> daughterNickNames(); // Key is always of type String
	
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
```

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
```java
interface Setter {
	void setName(String n); // modifies the field "name"
	void visible(boolean vsbl); // modifies the field "visible"
}
```
Set currently only support simple value types. 
Retrieve underlying jackson jsonNode like this:
```java
Twyn twyn = Twyn.forTest();
Contact contact = twyn.read(jsonResponse, Contact.class);
// modify contact...
JsonNode root = twyn.getJsonNode(contact);
	
```
##Todo:
* Better defined error handling
* Setter should optionally be able to return reference to this
* Support for other setters than value setters
* Smarter cache clearing when setting values