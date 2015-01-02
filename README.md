twyn!
==============
Take what you need!

Twyn is an extension to jackson json->java mapping framework which allows for lenient data parsing with less code.

Define your input data with interfaces and twyn will instantiate them for you and extract appropriate values from the underlying json data. 

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
	Adress getAddress();
	default String getCountry() {
		return getAddress().getCountry();
	}
}
```
