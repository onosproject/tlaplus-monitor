// SPDX-FileCopyrightText: 2020-present Open Networking Foundation <info@opennetworking.org>
//
// SPDX-License-Identifier: Apache-2.0

package tlc2.overrides;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import tlc2.value.IValue;
import tlc2.value.impl.BoolValue;
import tlc2.value.impl.IntValue;
import tlc2.value.impl.RecordValue;
import tlc2.value.impl.StringValue;
import tlc2.value.impl.TupleValue;
import tlc2.value.impl.Value;
import util.UniqueString;

/**
 * JSON utilities.
 */
public class JsonUtils {
  @TLAPlusOperator(identifier = "JsonDeserialize", module = "JsonUtils")
  public static IValue deserialize(final StringValue absolutePath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    BufferedReader reader = new BufferedReader(new FileReader(new File(absolutePath.val.toString())));
    List<Value> values = new ArrayList<>();
    try {
      String line = reader.readLine();
      while (line != null) {
        JsonNode node = mapper.readTree(line);
        values.add(getValue(node));
        line = reader.readLine();
      }
    } finally {
      reader.close();
    }
    return new TupleValue(values.toArray(new Value[0]));
  }

  @TLAPlusOperator(identifier = "JsonSerialize", module = "JsonUtils")
  public static void serialize(final StringValue absolutePath, final TupleValue value) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(absolutePath.val.toString())));
    try {
      for (int i = 0; i < value.elems.length; i++) {
        writer.write(getNode(value.elems[i]).toString() + "\n");
      }
    } finally {
      writer.close();
    }
  }

  static JsonNode getNode(IValue value) throws IOException {
    if (value instanceof RecordValue) {
      return getObjectNode((RecordValue) value);
    } else if (value instanceof TupleValue) {
      return getArrayNode((TupleValue) value);
    } else if (value instanceof StringValue) {
      return new TextNode(((StringValue) value).val.toString());
    } else if (value instanceof IntValue) {
      return new IntNode(((IntValue) value).val);
    } else if (value instanceof BoolValue) {
      return BooleanNode.valueOf(((BoolValue) value).val);
    } else {
      throw new IOException("Cannot convert value: Unknown type");
    }
  }

  private static JsonNode getObjectNode(RecordValue value) throws IOException {
    Map<String, JsonNode> entries = new HashMap<>();
    for (int i = 0; i < value.names.length; i++) {
      entries.put(value.names[i].toString(), getNode(value.values[i]));
    }
    return new ObjectNode(new JsonNodeFactory(true), entries);
  }

  private static JsonNode getArrayNode(TupleValue value) throws IOException {
    List<JsonNode> elements = new ArrayList<>(value.elems.length);
    for (int i = 0; i < value.elems.length; i++) {
      elements.add(getNode(value.elems[i]));
    }
    return new ArrayNode(new JsonNodeFactory(true), elements);
  }

  static Value getValue(JsonNode node) throws IOException {
    switch (node.getNodeType()) {
      case ARRAY:
        return getTupleValue(node);
      case OBJECT:
        return getRecordValue(node);
      case NUMBER:
        return IntValue.gen(node.asInt());
      case BOOLEAN:
        return new BoolValue(node.asBoolean());
      case STRING:
        return new StringValue(node.asText());
      case NULL:
        return null;
      default:
        throw new IOException("Cannot convert tuple value: Unknown type");
    }
  }

  private static TupleValue getTupleValue(JsonNode node) throws IOException {
    List<Value> values = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      values.add(getValue(node.get(i)));
    }
    return new TupleValue(values.toArray(new Value[0]));
  }

  private static RecordValue getRecordValue(JsonNode node) throws IOException {
    List<UniqueString> keys = new ArrayList<>();
    List<Value> values = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
    while (iterator.hasNext()) {
      Map.Entry<String, JsonNode> entry = iterator.next();
      keys.add(UniqueString.uniqueStringOf(entry.getKey()));
      values.add(getValue(entry.getValue()));
    }
    return new RecordValue(keys.toArray(new UniqueString[0]), values.toArray(new Value[0]), false);
  }
}
