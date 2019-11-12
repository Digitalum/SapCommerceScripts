def modelService = spring.getBean("modelService");
def flexService = spring.getBean("flexibleSearchService");

def items = flexService.search("select {pk} from {media} where {fileDescription} like '%png'");

items.getResult().each() { item ->
     item.setFileDescription(item.getFileDescription().replace('.jpg', '.png');

     println(item.getFileDescription());
     
     //modelService.save(item);
}
