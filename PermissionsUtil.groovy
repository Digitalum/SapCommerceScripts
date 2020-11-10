package permissions

import com.atlascopco.core.jalo.AtlascopcocoreManager
import com.google.common.collect.Lists
import de.hybris.platform.core.Registry
import de.hybris.platform.core.model.security.PrincipalModel
import de.hybris.platform.core.model.type.AttributeDescriptorModel
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.core.model.user.UserGroupModel
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery
import de.hybris.platform.servicelayer.search.FlexibleSearchService
import de.hybris.platform.servicelayer.security.permissions.PermissionAssignment
import de.hybris.platform.servicelayer.security.permissions.PermissionCheckingService
import de.hybris.platform.servicelayer.security.permissions.PermissionManagementService
import de.hybris.platform.servicelayer.type.TypeService

class PermissionsUtil {
    private TypeService typeService;
    private FlexibleSearchService flexibleSearchService;
    private ModelService modelService;
    private PermissionCheckingService permissionCheckingService;
    private PermissionManagementService permissionManagementService;

    PermissionsUtil() {
        println("Init PermissionsUtil");
        this.typeService = Registry.getGlobalApplicationContext().getBean("typeService", TypeService.class);
        this.flexibleSearchService = Registry.getGlobalApplicationContext().getBean("flexibleSearchService", FlexibleSearchService.class);
        this.modelService = Registry.getGlobalApplicationContext().getBean("modelService", ModelService.class);
        this.permissionCheckingService = Registry.getGlobalApplicationContext().getBean("permissionCheckingService", PermissionCheckingService.class);
        this.permissionManagementService = Registry.getGlobalApplicationContext().getBean("permissionManagementService", PermissionManagementService.class);
    }

    UserGroupModel getUserGroup(String code) {
        final FlexibleSearchQuery orderProcessesQuery = new FlexibleSearchQuery("SELECT {pk} FROM {UserGroup} WHERE {uid} = ?code");
        orderProcessesQuery.addQueryParameter("code", code);
        return flexibleSearchService.search(orderProcessesQuery).getResult().get(0);
    }

    ComposedTypeModel getItemModel(String code) {
        return typeService.getComposedTypeForCode(code);
    }

    void updateAttributes(String itemCode, String principalGroup) {
        print(itemCode);
        def group = new PermissionsUtil().getUserGroup(principalGroup);
        def productType = getItemModel(itemCode);

        def readChangeOnlyPermissions = createPermissons(group, true, true, false, false, false);
        permissionManagementService.addTypePermissions(productType, readChangeOnlyPermissions)
        printPermissions(permissions);
        for (AttributeDescriptorModel attributeDescriptorModel : productType.getDeclaredattributedescriptors()) {
            def readOnlyPermissions = createPermissons(group, true, false, false, false, false);
            println(attributeDescriptorModel.getQualifier());
            permissionManagementService.addAttributePermissions(attributeDescriptorModel, group, readOnlyPermissions);
        }
    }


    void updateAttributes(List<MyPermission> myPermissions, String principalGroup, boolean adminGroup) {
        println("Update attributes for : " + principalGroup)
        println("Permission Count " + myPermissions.size())
        def group = getUserGroup(principalGroup);
        Set<AttributeDescriptorModel> processedAttributes = new HashSet<>();
        Set<ComposedTypeModel> composedTypeModels = new HashSet<>();
        for (MyPermission myPermission : myPermissions) {
            print("Explicit Update " + myPermission.getType() + " --> ")
            def composedTypeModel = getItemModel(myPermission.getType().contains(".") ? myPermission.getType().split("\\.")[0] : myPermission.getType());
            println(composedTypeModel)
            composedTypeModels.add(composedTypeModel);
            def perms = createPermissons(group, myPermission.getRead(), myPermission.getChange(), myPermission.getCreate(), myPermission.getRemove(), myPermission.getChangePerm());
            if (!myPermission.getType().contains(".")) {
                permissionManagementService.addTypePermissions(composedTypeModel, perms)
            } else {
                String attributeQualifier = myPermission.getType().split("\\.")[1];
                println("-> '" + attributeQualifier + "'")
                def attributeDescriptorModel = typeService.getAttributeDescriptor(composedTypeModel, attributeQualifier);

                println("Implicit Update " + composedTypeModel.getCode())
                processedAttributes.add(attributeDescriptorModel);
                println(attributeDescriptorModel.getQualifier());
                permissionManagementService.addAttributePermissions(attributeDescriptorModel, perms);
            }
        }

        println("######################################################");
        for (ComposedTypeModel composedTypeModel : composedTypeModels) {

            println("Implicit Update " + composedTypeModel.getCode())
            Set<AttributeDescriptorModel> attributeDescriptorModels = typeService.getAttributeDescriptorsForType(composedTypeModel).findAll({ x -> !processedAttributes.contains(x) }).collect();
            println(attributeDescriptorModels.size())
            def canCreate1 = canCreate(composedTypeModel, group);
            for (AttributeDescriptorModel attributeDescriptorModel : attributeDescriptorModels) {

                println(" -- >  " + attributeDescriptorModel.getQualifier())
                def readChangeOnlyPermissions
                if (canCreate1) {
                    readChangeOnlyPermissions = createPermissons(group, true, true, true, true, false);
                } else {
                    readChangeOnlyPermissions = createPermissons(group, true, false, false, false, false);
                }
                permissionManagementService.addAttributePermissions(attributeDescriptorModel, readChangeOnlyPermissions);
            };
        }
        println("######################################################");
        println("######################################################");
    }

    private boolean canCreate(ComposedTypeModel composedTypeModel, PrincipalModel group) {
        boolean canCreate = false;
        Collection<PermissionAssignment> permissions = permissionManagementService.getTypePermissionsForPrincipal(composedTypeModel, group);
        for (PermissionAssignment permissionAssignment : permissions) {
            print(" -> ");
            if ("create".equals(permissionAssignment.getPermissionName()) && permissionAssignment.isGranted()) {
                canCreate = true;
            }
        }
        return canCreate;
    }

    void printAttributes(String itemCode, String principalGroup) {
        print(itemCode);
        def group = new PermissionsUtil().getUserGroup(principalGroup);
        def composedTypeModel = getItemModel(itemCode);
        Collection<PermissionAssignment> permissions = permissionManagementService.getTypePermissionsForPrincipal(composedTypeModel, group);
        printPermissions(permissions);
        for (AttributeDescriptorModel attributeDescriptorModel : composedTypeModel.getDeclaredattributedescriptors()) {
            println(attributeDescriptorModel.getQualifier());

            Collection<PermissionAssignment> permissions2 = permissionManagementService.getAttributePermissionsForPrincipal(attributeDescriptorModel, group);
            printPermissions(permissions2);
        }
    }

    private Collection<PermissionAssignment> createPermissons(UserGroupModel groupModel, boolean read, boolean change, boolean create, boolean remove, boolean changePerm) {
        Collection<PermissionAssignment> permissionAssignments = new ArrayList<>();

        permissionAssignments.add(new PermissionAssignment("read", groupModel, !read));
        permissionAssignments.add(new PermissionAssignment("change", groupModel, !change));
        permissionAssignments.add(new PermissionAssignment("create", groupModel, !create));
        permissionAssignments.add(new PermissionAssignment("remove", groupModel, !remove));
        permissionAssignments.add(new PermissionAssignment("changerights", groupModel, !changePerm));
        return permissionAssignments;
    }

    void printPermissions(Collection<PermissionAssignment> permissions) {
        for (PermissionAssignment permissionAssignment : permissions) {
            print(" -> ");
            print(permissionAssignment.getPermissionName());
            print(" -> ");
            println(permissionAssignment.isGranted());
        }
    }

    private List<MyPermission> readCsv(fileName) {
        //String fileName = "/permissions/permissionsMarketingGroup.csv";
        InputStream inputStream = AtlascopcocoreManager.class.getResourceAsStream(fileName);
        InputStreamReader isReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(isReader);
        String str;
        List<MyPermission> myPermissions = new ArrayList<>();
        while ((str = reader.readLine()) != null) {
            List<String> list = Lists.newArrayList(str.split(";")).stream().map({ x -> x.trim() }).collect();
            if (list.size() == 6 && !str.startsWith("#")) {
                myPermissions.add(new MyPermission(list.get(0).trim(), "+".equals(list.get(1)), "+".equals(list.get(2)), "+".equals(list.get(3)), "+".equals(list.get(4)), "+".equals(list.get(5))));
            }
        }
        return myPermissions;
    }

}

class MyPermission {
    private String type;
    private boolean read;
    private boolean change;
    private boolean create;
    private boolean remove;
    private boolean changePerm;

    MyPermission(String type, boolean read, boolean change, boolean create, boolean remove, boolean changePerm) {
        this.type = type
        this.read = read
        this.change = change
        this.create = create
        this.remove = remove
        this.changePerm = changePerm
    }

    String getType() {
        return type
    }

    boolean getRead() {
        return read
    }

    boolean getChange() {
        return change
    }

    boolean getCreate() {
        return create
    }

    boolean getRemove() {
        return remove
    }

    boolean getChangePerm() {
        return changePerm
    }
}

def util = new PermissionsUtil();

groups = ["AdminGroup", "MarketingGroup", "CommunicationsGroup"];
for (String groupName : groups) {
    println("#################################################################");
    println("Process Group : " + groupName);
    println("#################################################################");
    util.updateAttributes(defaultPerms, "ac" + groupName, groupName.contains("Admin"));
    List<MyPermission> perms = util.readCsv("/permissions/permissions" + groupName + ".csv");
    util.updateAttributes(perms, "ac" + groupName, groupName.contains("Admin"));
}

