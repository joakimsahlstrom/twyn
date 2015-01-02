twyn!
==============
Take what you need!

Twyn is an extension to jackson:s json->java mapping framework. It allows for lenient data parsing with less code.

Define your input data with interfaces and twyn will instantiate them for you and extract appropriate values from the underlying json data. 

Example:
--------
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
	Adress getAddress();
	default String getCountry() {
		return getAddress().getCountry();
	}
}
interface Address {
	String getCountry();
}

public void doStuff(InputStream jsonResponse) {
	Contact contact = new Twyn().read(jsonResponse, Contact.class);
	String firstName = contact.getFirstname();
	String country = contact.getCountry();
	System.out.println(firstName + " lives in " + country); // outputs "John lives in England"
}
```

What if, in the case given above, the json supplier decides to change "firstname" to "name_first" and your code has loads of references to Contact.getFirstname()? Easy!
```java
interface Contact {
	String getName_first();
	default String getFirstname() {
		return getName_first(); 
	}
	// The rest same as before...
}
```


Todo:
* Support for Collections
* Value caching

