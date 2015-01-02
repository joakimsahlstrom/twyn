# twyn!
Twyn maps json to java, using jackson under the hood. It allows for lenient data parsing with less code.

Requires Java 8. Currently relies on Proxy.newProxyInstance(...) so not performance is rather average.

##Features:
###Twyn only requires interfaces for mapping
Given this json where only firstname and country is interesting:
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
	Address getAddress();
}
interface Address {
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

// The rest same as above

public void doStuff(InputStream jsonResponse) {
	Contact contact = new Twyn().read(jsonResponse, Contact.class);
	Address address = contact.getAdress(); // This will return an Address instance by using the jackson java object mapper
}
```

###Twyn support JavaBeans style naming of properties
The json field "xxx" can be mapped by both getXxx, hasXxx but simple direct mapping xxx() works as well. Properties must match w/ correct case. 

##Todo:
* Value caching
* Improved performance (drop java proxies, use code generation instead?)
