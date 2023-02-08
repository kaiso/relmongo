package io.github.kaiso.relmongo.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.Map;

public class RelMongoProcessorRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableRelMongo.class.getName());
        if (annotationAttributes == null) {
            return;
        }
        String mongoTemplateRef = (String) annotationAttributes.get("mongoTemplateRef");
        Assert.notNull(mongoTemplateRef, "mongoTemplateRef in @EnableRelMongo must not be null!");

        BeanDefinitionBuilder postProcessorDefinitionBuilder = BeanDefinitionBuilder
            .rootBeanDefinition(RelMongoBeanPostProcessor.class);
        postProcessorDefinitionBuilder.addConstructorArgValue(mongoTemplateRef);

        String beanName = mongoTemplateRef + "$RelMongo$BeanPostProcessor";
        if (!registry.containsBeanDefinition(beanName)) {
            registry.registerBeanDefinition(beanName,
                postProcessorDefinitionBuilder.getBeanDefinition());
        }
        /*
         * BeanDefinitionBuilder relMongoProcessorDefinitionBuilder =
         * BeanDefinitionBuilder
         * .rootBeanDefinition(RelMongoProcessor.class);
         * 
         * relMongoProcessorDefinitionBuilder.addConstructorArgReference(
         * mongoTemplateRef);
         * 
         * AbstractBeanDefinition rlmpBeanDefinition =
         * relMongoProcessorDefinitionBuilder.getBeanDefinition();
         * 
         * String generateBeanName = BeanUtils.getProcessorBeanName(mongoTemplateRef);
         * 
         * if (!registry.containsBeanDefinition(generateBeanName)) {
         * registry.registerBeanDefinition(generateBeanName, rlmpBeanDefinition);
         * }
         */

        registerIndexCreator(registry, mongoTemplateRef);

    }

    private void registerIndexCreator(BeanDefinitionRegistry registry, String mongoTemplateRef) {

        String generateBeanName = mongoTemplateRef + "$RelMongo$OK$RLMIndexCreator";
        if (!registry.containsBeanDefinition(generateBeanName)) {
            BeanDefinitionBuilder idxCreatorDefinitionBuilder = BeanDefinitionBuilder
                .rootBeanDefinition(RelMongoPersistentEntityIndexCreator.class);

            idxCreatorDefinitionBuilder.addConstructorArgReference(mongoTemplateRef);
            idxCreatorDefinitionBuilder.setLazyInit(false);

            AbstractBeanDefinition idxCreatorDefinition = idxCreatorDefinitionBuilder.getBeanDefinition();
            registry.registerBeanDefinition(generateBeanName, idxCreatorDefinition);
        }

    }

}
