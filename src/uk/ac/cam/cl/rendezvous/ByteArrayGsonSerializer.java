package uk.ac.cam.cl.rendezvous;

import java.lang.reflect.Type;

import uk.ac.cam.cl.rendezvous.org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class ByteArrayGsonSerializer
    implements JsonDeserializer<byte[]>, JsonSerializer<byte[]> {

	@Override
	public JsonElement serialize(
	        byte[] b, Type type, JsonSerializationContext context) {
	    String s = Base64.encodeBase64String(b);
	    return new JsonPrimitive(s);
	}
	
	@Override
	public byte[] deserialize(
	        JsonElement json, Type type, JsonDeserializationContext context)
	        throws JsonParseException {
	    String s = json.getAsJsonPrimitive().getAsString();
	    return Base64.decodeBase64(s);
	}
}