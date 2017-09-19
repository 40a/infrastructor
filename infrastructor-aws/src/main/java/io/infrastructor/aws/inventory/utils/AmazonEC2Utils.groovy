package io.infrastructor.aws.inventory.utils

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.retry.RetryUtils
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeImagesResult
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.Instance
import io.infrastructor.aws.inventory.AwsNode

import static io.infrastructor.core.logging.ConsoleLogger.*
import static io.infrastructor.core.utils.RetryUtils.retry

class AmazonEC2Utils {
    
    def static AmazonEC2 amazonEC2(def awsAccessKey, def awsSecretKey, def awsRegion) {
        AmazonEC2ClientBuilder standard = AmazonEC2ClientBuilder.standard()
        standard.setCredentials(new AWSStaticCredentialsProvider(new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() { awsAccessKey }

                    @Override
                    public String getAWSSecretKey() { awsSecretKey }
                }))
        standard.setRegion(awsRegion)
        standard.build()
    }
    
    def static waitForInstanceState(def amazonEC2, def instanceId, int count, int delay, def state) {
        retry(count, delay) {
            DescribeInstancesRequest request = new DescribeInstancesRequest()
            request.setInstanceIds([instanceId])
            DescribeInstancesResult result = amazonEC2.describeInstances(request)
            Instance instance = result.getReservations().get(0).getInstances().get(0)
            debug "waiting for instance $instanceId state is $state, current state: ${instance.getState().getName()}"
            assert instance.getState().getName() == state
        }
    }
    
    def static waitForImageState(def amazonEC2, def imageId, int count, int delay, def state) {
        retry(count, delay) {
            DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest()
            describeImagesRequest.withImageIds(imageId)
            DescribeImagesResult describeImagesResult = amazonEC2.describeImages(describeImagesRequest)
            def actual = describeImagesResult.getImages().get(0).getState()
            debug "waiting for image $imageId is available, current state is $actual"
            assert actual == state
        }
    }
    
    public static void assertInstanceExists(def awsAccessKey, def awsSecretKey, def awsRegion, def definition) {
        def amazonEC2 = amazonEC2(awsAccessKey, awsSecretKey, awsRegion)
    
        def reservations = amazonEC2.describeInstances().getReservations()
        def allExistingRunningInstances = reservations.collect { 
            it.getInstances().findAll { 
                it.getState().getCode() == 16 // running
            }
        }.flatten()
    
        def expected = new AwsNode()
        expected.with(definition)
        
        def instance = allExistingRunningInstances.find { it.tags.find { it.key == 'Name' }?.value == expected.name }
        assert instance
        if (expected.imageId)          assert expected.imageId                   == instance.imageId
        if (expected.instanceType)     assert expected.instanceType              == instance.instanceType
        if (expected.subnetId)         assert expected.subnetId                  == instance.subnetId
        if (expected.keyName)          assert expected.keyName                   == instance.keyName
        if (expected.securityGroupIds) assert (expected.securityGroupIds as Set) == (instance.securityGroups.collect { it.groupId } as Set)
        if (expected.tags)             assert expected.tags                      == instance.tags.collectEntries { [(it.key as String) : (it.value as String)] } 
    }
    
    public static def findSubnetIdByName(def awsAccessKey, def awsSecretKey, def awsRegion, def name) {
        def amazonEC2 = amazonEC2(awsAccessKey, awsSecretKey, awsRegion) 
        def result = amazonEC2.describeSubnets(
            new DescribeSubnetsRequest().withFilters(new Filter("tag:Name", [name])))
         
        if (result.getSubnets().size() == 0) {
            throw new RuntimeException("Unable to find subnet with name '$name'")
        }
         
        if (result.getSubnets().size() > 1) {
            throw new RuntimeException("Multiple subnets with the same name ($name) has been found")
        }
        
        return result.getSubnets()[0].subnetId
    }
}
