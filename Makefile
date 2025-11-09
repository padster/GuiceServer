build:
  # Actually compile the code
	mvn clean compile

build_and_update: build
  # set it up in local cache so other project builds will use it.
	mvn install -DskipTests
	
# mvn install -DskipTests
