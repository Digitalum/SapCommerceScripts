def extensionName = "myCustomExtensionWithItemsXml";

def modelService = spring.getBean("modelService");
def flexService = spring.getBean("flexibleSearchService");

def items = flexService.search("SELECT {pk} from {ComposedType} ORDER BY {code}");

items.getResult().each() { item ->
     boolean hasValue = false;
     if (extensionName.equals(item.getExtensionName())) {
          println("type." + item.getCode() + ".name=" + (item.getName() == null ? "" : item.getName()));
          println("type." + item.getCode() + ".description=" + (item.getDescription() == null ? "" : item.getDescription()));
          hasValue = true;
     }

     item.getDeclaredattributedescriptors().each() { descriptor ->
          if (extensionName.equals(descriptor.getExtensionName())) {
               println("type." + item.getCode() + "." + descriptor.getQualifier() + ".name=" + (descriptor.getName() == null ? "" : descriptor.getName()));
               println("type." + item.getCode() + "." + descriptor.getQualifier() + ".description=" + (descriptor.getDescription() == null ? "" : descriptor.getDescription()));
               hasValue = true;
          }
     }
     if (hasValue) {
          println();
     }
}
