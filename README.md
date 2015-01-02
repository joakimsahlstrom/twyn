twyn!
==============
Take what you need!

Twyn is an extension to jackson json->java mapping framework which allows for lenient data parsing with less code.

Define your input data with interfaces and twyn will 

```json
{
	"firstname" : "John",
	"lastname" : "Doe",
	"age" : "38"
	"address" : {
		"street" : "Street 1",
		"city" : "London",
		"country" : "England"
	}
}
```

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
	Contact contact = twyn.read(jsonResponse, Contact.class);
	String firstName = contact.getFirstname();
	String country = contact.getCountry();
	// do stuff!
}
```