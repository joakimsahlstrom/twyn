# twyn!
Twyn maps json to java, using jackson under the hood. It allows for lenient parsing with less code.

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
The json field "xxx" can be mapped by both getXxx, hasXxx but simple direct mapping xxx() works as well. Properties must match w/ correct case. 

###Twyn supports arrays and collections
```json
{
	"daughters" : [ { "name" : "Inara" }, { "name" : "Kaylee" }, { "name" : "River" } ],
	"daughterNickNames" : {
		"Inara" : { "nick" : "innie" },
		"Kaylee" : { "nick" : "lee" }
	},
	"sons" : [ "Mal", "Wash" ],
	"unknowns" : [ { "name" : "Chtulu", "type" : "squid" }, { "name" : "Donald", "type" : "duck" } ]
}
```
Can be mapped with:
```java
interface Offspring {
	Daughter[] daughters();
	
	@TwynCollection(Nick.class)
	Map<String, Nick> daughterNickNames();
	
	String[] sons();
	
	@TwynCollection(Entity.class)
	List<Entity> getUnknowns();
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
```

###Twyn can use either java proxies or runtime-generated code for object creation
```java
Twyn.configurer().withClassGeneration().configure();
Twyn.configurer().withJavaProxies().configure();
// Alternate ObjectMappers can be used
Twyn.configurer().withClassGeneration().withObjectMapper(myObjectMapper).configure();
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

##Todo:
* .equals & .hashCode support for proxies (@Id for reference objects, no annotation for value objects)
* Support Set
* Pre-compilation of classes
* Value caching
* Support for setters