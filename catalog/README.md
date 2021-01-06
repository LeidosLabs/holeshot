# Deploy
1. `. scripts/deployCatalog.sh stackSuffix` where stackSuffix is a unique identifier for the stack (e.g. branch name)

# Post-Deploy steps
1. Update the imagery index in ElasticSearch
  1. Look up the domain name for the ES instance (more easily done via cloudformation -> stack -> look for the ES stack -> navigate to resource -> will be something like https://vpc-image-catalog-stackSuffix-bunchOfRandomLetters.us-east-1.es.amazonaws.com/)
  2. Log into the bastion and issue a PUT the json in elasticsearch/ImageCatalogMapping.json to ESInstanceUrl/imagery (e.g. https://vpc-image-catalog-stackSuffix-bunchOfRandomLetters.us-east-1.es.amazonaws.com/imagery)
2. Add the deployed ImageCatalogAPI (imagecatalog stage) to the IRADDevelopmentTeam usage plan