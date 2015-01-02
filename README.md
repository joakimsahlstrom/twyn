# twyn!
Twyn maps json to java, using jackson under the hood. It allows for lenient parsing with less code.

Requires Java 8. Currently relies on java proxies so performance is rather average.

##Features:
###Twyn only requires interfaces for mapping
Given this json where only "firstname" and "country" is interesting:
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
Define these interfaces and parse like below:
```java
interface Contact {
	String getFirstname();
	AddressInfo getAddress();
}
interface AddressInfo {
	String getCountry();
}

public void doStuff(InputStream jsonResponse) {
	Contact contact = new Twyn().read(jsonResponse, Contact.class);
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
	Contact contact = new Twyn().read(jsonResponse, Contact.class);
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

##Todo:
* Value caching
* Improved performance (drop java proxies, use code generation instead?)
* Support for setters