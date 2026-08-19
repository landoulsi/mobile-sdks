#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SharedAddress, SharedCardNetwork, SharedCardNetworkCompanion, SharedCurrency, SharedCurrencyCompanion, SharedGooglePayAuthMethod, SharedGooglePayBillingAddressFormat, SharedGooglePayBillingAddressParameters, SharedGooglePayConfig, SharedGooglePayEnvironment, SharedGooglePayShippingAddressParameters, SharedGooglePayTokenizationSpecificationDirect, SharedGooglePayTokenizationSpecificationGateway, SharedGooglePayTokenizationSpecificationGatewayCompanion, SharedKotlinArray<T>, SharedKotlinEnum<E>, SharedKotlinEnumCompanion, SharedKotlinException, SharedKotlinIllegalStateException, SharedKotlinRuntimeException, SharedKotlinThrowable, SharedMoney, SharedMoneyCompanion, SharedPaymentErrorCode, SharedPaymentMethodAlternative, SharedPaymentMethodApplePay, SharedPaymentMethodCard, SharedPaymentMethodGooglePay, SharedPaymentMethodPayPal, SharedPaymentMethodType, SharedPaymentRequest, SharedPaymentResultCanceled, SharedPaymentResultFailure, SharedPaymentResultSuccess;

@protocol SharedGooglePayTokenizationSpecification, SharedKotlinComparable, SharedKotlinIterator, SharedPaymentMethod, SharedPaymentResult;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface SharedBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SharedBase (SharedBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SharedMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SharedMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSharedKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SharedNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface SharedByte : SharedNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SharedUByte : SharedNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SharedShort : SharedNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SharedUShort : SharedNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SharedInt : SharedNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SharedUInt : SharedNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SharedLong : SharedNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SharedULong : SharedNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SharedFloat : SharedNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SharedDouble : SharedNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SharedBoolean : SharedNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Platform")))
@interface SharedPlatform : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) NSString *platform __attribute__((swift_name("platform")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Address")))
@interface SharedAddress : SharedBase
- (instancetype)initWithName:(NSString * _Nullable)name address1:(NSString * _Nullable)address1 address2:(NSString * _Nullable)address2 city:(NSString * _Nullable)city state:(NSString * _Nullable)state postalCode:(NSString * _Nullable)postalCode countryCode:(NSString * _Nullable)countryCode phoneNumber:(NSString * _Nullable)phoneNumber email:(NSString * _Nullable)email __attribute__((swift_name("init(name:address1:address2:city:state:postalCode:countryCode:phoneNumber:email:)"))) __attribute__((objc_designated_initializer));
- (SharedAddress *)doCopyName:(NSString * _Nullable)name address1:(NSString * _Nullable)address1 address2:(NSString * _Nullable)address2 city:(NSString * _Nullable)city state:(NSString * _Nullable)state postalCode:(NSString * _Nullable)postalCode countryCode:(NSString * _Nullable)countryCode phoneNumber:(NSString * _Nullable)phoneNumber email:(NSString * _Nullable)email __attribute__((swift_name("doCopy(name:address1:address2:city:state:postalCode:countryCode:phoneNumber:email:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable address1 __attribute__((swift_name("address1")));
@property (readonly) NSString * _Nullable address2 __attribute__((swift_name("address2")));
@property (readonly) NSString * _Nullable city __attribute__((swift_name("city")));
@property (readonly) NSString * _Nullable countryCode __attribute__((swift_name("countryCode")));
@property (readonly) NSString * _Nullable email __attribute__((swift_name("email")));
@property (readonly) NSString * _Nullable name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable phoneNumber __attribute__((swift_name("phoneNumber")));
@property (readonly) NSString * _Nullable postalCode __attribute__((swift_name("postalCode")));
@property (readonly) NSString * _Nullable state __attribute__((swift_name("state")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SharedKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SharedKotlinEnum<E> : SharedBase <SharedKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CardNetwork")))
@interface SharedCardNetwork : SharedKotlinEnum<SharedCardNetwork *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedCardNetworkCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedCardNetwork *visa __attribute__((swift_name("visa")));
@property (class, readonly) SharedCardNetwork *mastercard __attribute__((swift_name("mastercard")));
@property (class, readonly) SharedCardNetwork *amex __attribute__((swift_name("amex")));
@property (class, readonly) SharedCardNetwork *discover __attribute__((swift_name("discover")));
@property (class, readonly) SharedCardNetwork *jcb __attribute__((swift_name("jcb")));
@property (class, readonly) SharedCardNetwork *interac __attribute__((swift_name("interac")));
@property (class, readonly) SharedCardNetwork *dinersClub __attribute__((swift_name("dinersClub")));
@property (class, readonly) SharedCardNetwork *unionPay __attribute__((swift_name("unionPay")));
+ (SharedKotlinArray<SharedCardNetwork *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCardNetwork *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *networkName __attribute__((swift_name("networkName")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CardNetwork.Companion")))
@interface SharedCardNetworkCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCardNetworkCompanion *shared __attribute__((swift_name("shared")));
- (SharedCardNetwork * _Nullable)fromNameName:(NSString *)name __attribute__((swift_name("fromName(name:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Currency")))
@interface SharedCurrency : SharedBase
- (instancetype)initWithCode:(NSString *)code symbol:(NSString *)symbol decimalPlaces:(int32_t)decimalPlaces __attribute__((swift_name("init(code:symbol:decimalPlaces:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedCurrencyCompanion *companion __attribute__((swift_name("companion")));
- (SharedCurrency *)doCopyCode:(NSString *)code symbol:(NSString *)symbol decimalPlaces:(int32_t)decimalPlaces __attribute__((swift_name("doCopy(code:symbol:decimalPlaces:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) int32_t decimalPlaces __attribute__((swift_name("decimalPlaces")));
@property (readonly) NSString *symbol __attribute__((swift_name("symbol")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Currency.Companion")))
@interface SharedCurrencyCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCurrencyCompanion *shared __attribute__((swift_name("shared")));
- (SharedCurrency *)fromCodeCode:(NSString *)code __attribute__((swift_name("fromCode(code:)")));
@property (readonly) SharedCurrency *AED __attribute__((swift_name("AED")));
@property (readonly) SharedCurrency *AUD __attribute__((swift_name("AUD")));
@property (readonly) SharedCurrency *CAD __attribute__((swift_name("CAD")));
@property (readonly) SharedCurrency *CHF __attribute__((swift_name("CHF")));
@property (readonly) SharedCurrency *EUR __attribute__((swift_name("EUR")));
@property (readonly) SharedCurrency *GBP __attribute__((swift_name("GBP")));
@property (readonly) SharedCurrency *JPY __attribute__((swift_name("JPY")));
@property (readonly) SharedCurrency *SAR __attribute__((swift_name("SAR")));
@property (readonly) SharedCurrency *USD __attribute__((swift_name("USD")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayAuthMethod")))
@interface SharedGooglePayAuthMethod : SharedKotlinEnum<SharedGooglePayAuthMethod *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedGooglePayAuthMethod *panOnly __attribute__((swift_name("panOnly")));
@property (class, readonly) SharedGooglePayAuthMethod *cryptogram3ds __attribute__((swift_name("cryptogram3ds")));
+ (SharedKotlinArray<SharedGooglePayAuthMethod *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedGooglePayAuthMethod *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayBillingAddressFormat")))
@interface SharedGooglePayBillingAddressFormat : SharedKotlinEnum<SharedGooglePayBillingAddressFormat *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedGooglePayBillingAddressFormat *min __attribute__((swift_name("min")));
@property (class, readonly) SharedGooglePayBillingAddressFormat *full __attribute__((swift_name("full")));
+ (SharedKotlinArray<SharedGooglePayBillingAddressFormat *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedGooglePayBillingAddressFormat *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayBillingAddressParameters")))
@interface SharedGooglePayBillingAddressParameters : SharedBase
- (instancetype)initWithFormat:(SharedGooglePayBillingAddressFormat *)format phoneNumberRequired:(BOOL)phoneNumberRequired __attribute__((swift_name("init(format:phoneNumberRequired:)"))) __attribute__((objc_designated_initializer));
- (SharedGooglePayBillingAddressParameters *)doCopyFormat:(SharedGooglePayBillingAddressFormat *)format phoneNumberRequired:(BOOL)phoneNumberRequired __attribute__((swift_name("doCopy(format:phoneNumberRequired:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedGooglePayBillingAddressFormat *format __attribute__((swift_name("format")));
@property (readonly) BOOL phoneNumberRequired __attribute__((swift_name("phoneNumberRequired")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayConfig")))
@interface SharedGooglePayConfig : SharedBase
- (instancetype)initWithEnvironment:(SharedGooglePayEnvironment *)environment merchantId:(NSString *)merchantId merchantName:(NSString *)merchantName allowedCardNetworks:(NSArray<SharedCardNetwork *> *)allowedCardNetworks allowedAuthMethods:(NSArray<SharedGooglePayAuthMethod *> *)allowedAuthMethods tokenizationSpecification:(id<SharedGooglePayTokenizationSpecification>)tokenizationSpecification allowPrepaidCards:(BOOL)allowPrepaidCards allowCreditCards:(BOOL)allowCreditCards billingAddressRequired:(BOOL)billingAddressRequired billingAddressParameters:(SharedGooglePayBillingAddressParameters * _Nullable)billingAddressParameters emailRequired:(BOOL)emailRequired shippingAddressRequired:(BOOL)shippingAddressRequired shippingAddressParameters:(SharedGooglePayShippingAddressParameters * _Nullable)shippingAddressParameters __attribute__((swift_name("init(environment:merchantId:merchantName:allowedCardNetworks:allowedAuthMethods:tokenizationSpecification:allowPrepaidCards:allowCreditCards:billingAddressRequired:billingAddressParameters:emailRequired:shippingAddressRequired:shippingAddressParameters:)"))) __attribute__((objc_designated_initializer));
- (SharedGooglePayConfig *)doCopyEnvironment:(SharedGooglePayEnvironment *)environment merchantId:(NSString *)merchantId merchantName:(NSString *)merchantName allowedCardNetworks:(NSArray<SharedCardNetwork *> *)allowedCardNetworks allowedAuthMethods:(NSArray<SharedGooglePayAuthMethod *> *)allowedAuthMethods tokenizationSpecification:(id<SharedGooglePayTokenizationSpecification>)tokenizationSpecification allowPrepaidCards:(BOOL)allowPrepaidCards allowCreditCards:(BOOL)allowCreditCards billingAddressRequired:(BOOL)billingAddressRequired billingAddressParameters:(SharedGooglePayBillingAddressParameters * _Nullable)billingAddressParameters emailRequired:(BOOL)emailRequired shippingAddressRequired:(BOOL)shippingAddressRequired shippingAddressParameters:(SharedGooglePayShippingAddressParameters * _Nullable)shippingAddressParameters __attribute__((swift_name("doCopy(environment:merchantId:merchantName:allowedCardNetworks:allowedAuthMethods:tokenizationSpecification:allowPrepaidCards:allowCreditCards:billingAddressRequired:billingAddressParameters:emailRequired:shippingAddressRequired:shippingAddressParameters:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL allowCreditCards __attribute__((swift_name("allowCreditCards")));
@property (readonly) BOOL allowPrepaidCards __attribute__((swift_name("allowPrepaidCards")));
@property (readonly) NSArray<SharedGooglePayAuthMethod *> *allowedAuthMethods __attribute__((swift_name("allowedAuthMethods")));
@property (readonly) NSArray<SharedCardNetwork *> *allowedCardNetworks __attribute__((swift_name("allowedCardNetworks")));
@property (readonly) SharedGooglePayBillingAddressParameters * _Nullable billingAddressParameters __attribute__((swift_name("billingAddressParameters")));
@property (readonly) BOOL billingAddressRequired __attribute__((swift_name("billingAddressRequired")));
@property (readonly) BOOL emailRequired __attribute__((swift_name("emailRequired")));
@property (readonly) SharedGooglePayEnvironment *environment __attribute__((swift_name("environment")));
@property (readonly) NSString *merchantId __attribute__((swift_name("merchantId")));
@property (readonly) NSString *merchantName __attribute__((swift_name("merchantName")));
@property (readonly) SharedGooglePayShippingAddressParameters * _Nullable shippingAddressParameters __attribute__((swift_name("shippingAddressParameters")));
@property (readonly) BOOL shippingAddressRequired __attribute__((swift_name("shippingAddressRequired")));
@property (readonly) id<SharedGooglePayTokenizationSpecification> tokenizationSpecification __attribute__((swift_name("tokenizationSpecification")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayEnvironment")))
@interface SharedGooglePayEnvironment : SharedKotlinEnum<SharedGooglePayEnvironment *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedGooglePayEnvironment *test __attribute__((swift_name("test")));
@property (class, readonly) SharedGooglePayEnvironment *production __attribute__((swift_name("production")));
+ (SharedKotlinArray<SharedGooglePayEnvironment *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedGooglePayEnvironment *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayShippingAddressParameters")))
@interface SharedGooglePayShippingAddressParameters : SharedBase
- (instancetype)initWithAllowedCountryCodes:(NSArray<NSString *> *)allowedCountryCodes phoneNumberRequired:(BOOL)phoneNumberRequired __attribute__((swift_name("init(allowedCountryCodes:phoneNumberRequired:)"))) __attribute__((objc_designated_initializer));
- (SharedGooglePayShippingAddressParameters *)doCopyAllowedCountryCodes:(NSArray<NSString *> *)allowedCountryCodes phoneNumberRequired:(BOOL)phoneNumberRequired __attribute__((swift_name("doCopy(allowedCountryCodes:phoneNumberRequired:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *allowedCountryCodes __attribute__((swift_name("allowedCountryCodes")));
@property (readonly) BOOL phoneNumberRequired __attribute__((swift_name("phoneNumberRequired")));
@end

__attribute__((swift_name("GooglePayTokenizationSpecification")))
@protocol SharedGooglePayTokenizationSpecification
@required
@property (readonly) NSDictionary<NSString *, NSString *> *parameters __attribute__((swift_name("parameters")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayTokenizationSpecificationDirect")))
@interface SharedGooglePayTokenizationSpecificationDirect : SharedBase <SharedGooglePayTokenizationSpecification>
- (instancetype)initWithPublicKey:(NSString *)publicKey protocolVersion:(NSString *)protocolVersion __attribute__((swift_name("init(publicKey:protocolVersion:)"))) __attribute__((objc_designated_initializer));
- (SharedGooglePayTokenizationSpecificationDirect *)doCopyPublicKey:(NSString *)publicKey protocolVersion:(NSString *)protocolVersion __attribute__((swift_name("doCopy(publicKey:protocolVersion:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, NSString *> *parameters __attribute__((swift_name("parameters")));
@property (readonly) NSString *protocolVersion __attribute__((swift_name("protocolVersion")));
@property (readonly) NSString *publicKey __attribute__((swift_name("publicKey")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayTokenizationSpecificationGateway")))
@interface SharedGooglePayTokenizationSpecificationGateway : SharedBase <SharedGooglePayTokenizationSpecification>
- (instancetype)initWithGateway:(NSString *)gateway gatewayMerchantId:(NSString *)gatewayMerchantId extraParameters:(NSDictionary<NSString *, NSString *> *)extraParameters __attribute__((swift_name("init(gateway:gatewayMerchantId:extraParameters:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedGooglePayTokenizationSpecificationGatewayCompanion *companion __attribute__((swift_name("companion")));
- (SharedGooglePayTokenizationSpecificationGateway *)doCopyGateway:(NSString *)gateway gatewayMerchantId:(NSString *)gatewayMerchantId extraParameters:(NSDictionary<NSString *, NSString *> *)extraParameters __attribute__((swift_name("doCopy(gateway:gatewayMerchantId:extraParameters:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, NSString *> *extraParameters __attribute__((swift_name("extraParameters")));
@property (readonly) NSString *gateway __attribute__((swift_name("gateway")));
@property (readonly) NSString *gatewayMerchantId __attribute__((swift_name("gatewayMerchantId")));
@property (readonly) NSDictionary<NSString *, NSString *> *parameters __attribute__((swift_name("parameters")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GooglePayTokenizationSpecificationGateway.Companion")))
@interface SharedGooglePayTokenizationSpecificationGatewayCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedGooglePayTokenizationSpecificationGatewayCompanion *shared __attribute__((swift_name("shared")));
- (SharedGooglePayTokenizationSpecificationGateway *)adyenGatewayMerchantId:(NSString *)gatewayMerchantId __attribute__((swift_name("adyen(gatewayMerchantId:)")));
- (SharedGooglePayTokenizationSpecificationGateway *)braintreeTokenizationKey:(NSString *)tokenizationKey braintreeVersion:(NSString *)braintreeVersion __attribute__((swift_name("braintree(tokenizationKey:braintreeVersion:)")));
- (SharedGooglePayTokenizationSpecificationGateway *)stripePublishableKey:(NSString *)publishableKey stripeVersion:(NSString *)stripeVersion __attribute__((swift_name("stripe(publishableKey:stripeVersion:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Money")))
@interface SharedMoney : SharedBase <SharedKotlinComparable>
- (instancetype)initWithAmountMinorUnits:(int64_t)amountMinorUnits currency:(SharedCurrency *)currency __attribute__((swift_name("init(amountMinorUnits:currency:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMoneyCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedMoney *)other __attribute__((swift_name("compareTo(other:)")));
- (SharedMoney *)doCopyAmountMinorUnits:(int64_t)amountMinorUnits currency:(SharedCurrency *)currency __attribute__((swift_name("doCopy(amountMinorUnits:currency:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSString *)formattedAmount __attribute__((swift_name("formattedAmount()")));
- (NSString *)formattedWithSymbol __attribute__((swift_name("formattedWithSymbol()")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedMoney *)minusOther:(SharedMoney *)other __attribute__((swift_name("minus(other:)")));
- (SharedMoney *)plusOther:(SharedMoney *)other __attribute__((swift_name("plus(other:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t amountMinorUnits __attribute__((swift_name("amountMinorUnits")));
@property (readonly) SharedCurrency *currency __attribute__((swift_name("currency")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Money.Companion")))
@interface SharedMoneyCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMoneyCompanion *shared __attribute__((swift_name("shared")));
- (SharedMoney *)fromMajorUnitsAmount:(double)amount currency:(SharedCurrency *)currency __attribute__((swift_name("fromMajorUnits(amount:currency:)")));
- (SharedMoney *)fromMajorUnitsAmount:(int64_t)amount currency_:(SharedCurrency *)currency __attribute__((swift_name("fromMajorUnits(amount:currency_:)")));
- (SharedMoney *)ofCentsCents:(int64_t)cents currency:(SharedCurrency *)currency __attribute__((swift_name("ofCents(cents:currency:)")));
@property (readonly) SharedMoney *ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentErrorCode")))
@interface SharedPaymentErrorCode : SharedKotlinEnum<SharedPaymentErrorCode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedPaymentErrorCode *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) SharedPaymentErrorCode *networkError __attribute__((swift_name("networkError")));
@property (class, readonly) SharedPaymentErrorCode *gatewayError __attribute__((swift_name("gatewayError")));
@property (class, readonly) SharedPaymentErrorCode *configurationError __attribute__((swift_name("configurationError")));
@property (class, readonly) SharedPaymentErrorCode *paymentMethodUnavailable __attribute__((swift_name("paymentMethodUnavailable")));
@property (class, readonly) SharedPaymentErrorCode *cardDeclined __attribute__((swift_name("cardDeclined")));
@property (class, readonly) SharedPaymentErrorCode *expiredCard __attribute__((swift_name("expiredCard")));
@property (class, readonly) SharedPaymentErrorCode *insufficientFunds __attribute__((swift_name("insufficientFunds")));
@property (class, readonly) SharedPaymentErrorCode *authenticationFailed __attribute__((swift_name("authenticationFailed")));
@property (class, readonly) SharedPaymentErrorCode *userCanceled __attribute__((swift_name("userCanceled")));
+ (SharedKotlinArray<SharedPaymentErrorCode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedPaymentErrorCode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("PaymentMethod")))
@protocol SharedPaymentMethod
@required
@property (readonly) SharedPaymentMethodType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentMethodAlternative")))
@interface SharedPaymentMethodAlternative : SharedBase <SharedPaymentMethod>
- (instancetype)initWithType:(SharedPaymentMethodType *)type parameters:(NSDictionary<NSString *, NSString *> *)parameters __attribute__((swift_name("init(type:parameters:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentMethodAlternative *)doCopyType:(SharedPaymentMethodType *)type parameters:(NSDictionary<NSString *, NSString *> *)parameters __attribute__((swift_name("doCopy(type:parameters:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, NSString *> *parameters __attribute__((swift_name("parameters")));
@property (readonly) SharedPaymentMethodType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentMethodApplePay")))
@interface SharedPaymentMethodApplePay : SharedBase <SharedPaymentMethod>
- (instancetype)initWithMerchantId:(NSString *)merchantId countryCode:(NSString *)countryCode __attribute__((swift_name("init(merchantId:countryCode:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentMethodApplePay *)doCopyMerchantId:(NSString *)merchantId countryCode:(NSString *)countryCode __attribute__((swift_name("doCopy(merchantId:countryCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *countryCode __attribute__((swift_name("countryCode")));
@property (readonly) NSString *merchantId __attribute__((swift_name("merchantId")));
@property (readonly) SharedPaymentMethodType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentMethodCard")))
@interface SharedPaymentMethodCard : SharedBase <SharedPaymentMethod>
- (instancetype)initWithNumber:(NSString *)number expiryMonth:(int32_t)expiryMonth expiryYear:(int32_t)expiryYear cvc:(NSString *)cvc cardholderName:(NSString * _Nullable)cardholderName __attribute__((swift_name("init(number:expiryMonth:expiryYear:cvc:cardholderName:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentMethodCard *)doCopyNumber:(NSString *)number expiryMonth:(int32_t)expiryMonth expiryYear:(int32_t)expiryYear cvc:(NSString *)cvc cardholderName:(NSString * _Nullable)cardholderName __attribute__((swift_name("doCopy(number:expiryMonth:expiryYear:cvc:cardholderName:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable cardholderName __attribute__((swift_name("cardholderName")));
@property (readonly) NSString *cvc __attribute__((swift_name("cvc")));
@property (readonly) int32_t expiryMonth __attribute__((swift_name("expiryMonth")));
@property (readonly) int32_t expiryYear __attribute__((swift_name("expiryYear")));
@property (readonly) NSString *number __attribute__((swift_name("number")));
@property (readonly) SharedPaymentMethodType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentMethodGooglePay")))
@interface SharedPaymentMethodGooglePay : SharedBase <SharedPaymentMethod>
- (instancetype)initWithConfig:(SharedGooglePayConfig *)config __attribute__((swift_name("init(config:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentMethodGooglePay *)doCopyConfig:(SharedGooglePayConfig *)config __attribute__((swift_name("doCopy(config:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedGooglePayConfig *config __attribute__((swift_name("config")));
@property (readonly) SharedPaymentMethodType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentMethodPayPal")))
@interface SharedPaymentMethodPayPal : SharedBase <SharedPaymentMethod>
- (instancetype)initWithAccountId:(NSString * _Nullable)accountId __attribute__((swift_name("init(accountId:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentMethodPayPal *)doCopyAccountId:(NSString * _Nullable)accountId __attribute__((swift_name("doCopy(accountId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable accountId __attribute__((swift_name("accountId")));
@property (readonly) SharedPaymentMethodType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentMethodType")))
@interface SharedPaymentMethodType : SharedKotlinEnum<SharedPaymentMethodType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedPaymentMethodType *googlePay __attribute__((swift_name("googlePay")));
@property (class, readonly) SharedPaymentMethodType *applePay __attribute__((swift_name("applePay")));
@property (class, readonly) SharedPaymentMethodType *card __attribute__((swift_name("card")));
@property (class, readonly) SharedPaymentMethodType *paypal __attribute__((swift_name("paypal")));
@property (class, readonly) SharedPaymentMethodType *klarna __attribute__((swift_name("klarna")));
@property (class, readonly) SharedPaymentMethodType *ideal __attribute__((swift_name("ideal")));
+ (SharedKotlinArray<SharedPaymentMethodType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedPaymentMethodType *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *identifier __attribute__((swift_name("identifier")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentRequest")))
@interface SharedPaymentRequest : SharedBase
- (instancetype)initWithId:(NSString *)id amount:(SharedMoney *)amount merchantName:(NSString * _Nullable)merchantName description:(NSString * _Nullable)description allowedPaymentMethods:(NSArray<SharedPaymentMethodType *> *)allowedPaymentMethods googlePayConfig:(SharedGooglePayConfig * _Nullable)googlePayConfig requireShipping:(BOOL)requireShipping requireBillingAddress:(BOOL)requireBillingAddress metadata:(NSDictionary<NSString *, NSString *> *)metadata __attribute__((swift_name("init(id:amount:merchantName:description:allowedPaymentMethods:googlePayConfig:requireShipping:requireBillingAddress:metadata:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentRequest *)doCopyId:(NSString *)id amount:(SharedMoney *)amount merchantName:(NSString * _Nullable)merchantName description:(NSString * _Nullable)description allowedPaymentMethods:(NSArray<SharedPaymentMethodType *> *)allowedPaymentMethods googlePayConfig:(SharedGooglePayConfig * _Nullable)googlePayConfig requireShipping:(BOOL)requireShipping requireBillingAddress:(BOOL)requireBillingAddress metadata:(NSDictionary<NSString *, NSString *> *)metadata __attribute__((swift_name("doCopy(id:amount:merchantName:description:allowedPaymentMethods:googlePayConfig:requireShipping:requireBillingAddress:metadata:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedPaymentMethodType *> *allowedPaymentMethods __attribute__((swift_name("allowedPaymentMethods")));
@property (readonly) SharedMoney *amount __attribute__((swift_name("amount")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) SharedGooglePayConfig * _Nullable googlePayConfig __attribute__((swift_name("googlePayConfig")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable merchantName __attribute__((swift_name("merchantName")));
@property (readonly) NSDictionary<NSString *, NSString *> *metadata __attribute__((swift_name("metadata")));
@property (readonly) BOOL requireBillingAddress __attribute__((swift_name("requireBillingAddress")));
@property (readonly) BOOL requireShipping __attribute__((swift_name("requireShipping")));
@end

__attribute__((swift_name("PaymentResult")))
@protocol SharedPaymentResult
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentResultCanceled")))
@interface SharedPaymentResultCanceled : SharedBase <SharedPaymentResult>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)canceled __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPaymentResultCanceled *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentResultFailure")))
@interface SharedPaymentResultFailure : SharedBase <SharedPaymentResult>
- (instancetype)initWithErrorCode:(SharedPaymentErrorCode *)errorCode message:(NSString *)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(errorCode:message:cause:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentResultFailure *)doCopyErrorCode:(SharedPaymentErrorCode *)errorCode message:(NSString *)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("doCopy(errorCode:message:cause:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) SharedPaymentErrorCode *errorCode __attribute__((swift_name("errorCode")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaymentResultSuccess")))
@interface SharedPaymentResultSuccess : SharedBase <SharedPaymentResult>
- (instancetype)initWithTransactionId:(NSString *)transactionId paymentMethodType:(SharedPaymentMethodType *)paymentMethodType token:(NSString * _Nullable)token rawPaymentData:(NSString * _Nullable)rawPaymentData last4:(NSString * _Nullable)last4 cardNetwork:(SharedCardNetwork * _Nullable)cardNetwork billingAddress:(SharedAddress * _Nullable)billingAddress shippingAddress:(SharedAddress * _Nullable)shippingAddress email:(NSString * _Nullable)email __attribute__((swift_name("init(transactionId:paymentMethodType:token:rawPaymentData:last4:cardNetwork:billingAddress:shippingAddress:email:)"))) __attribute__((objc_designated_initializer));
- (SharedPaymentResultSuccess *)doCopyTransactionId:(NSString *)transactionId paymentMethodType:(SharedPaymentMethodType *)paymentMethodType token:(NSString * _Nullable)token rawPaymentData:(NSString * _Nullable)rawPaymentData last4:(NSString * _Nullable)last4 cardNetwork:(SharedCardNetwork * _Nullable)cardNetwork billingAddress:(SharedAddress * _Nullable)billingAddress shippingAddress:(SharedAddress * _Nullable)shippingAddress email:(NSString * _Nullable)email __attribute__((swift_name("doCopy(transactionId:paymentMethodType:token:rawPaymentData:last4:cardNetwork:billingAddress:shippingAddress:email:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAddress * _Nullable billingAddress __attribute__((swift_name("billingAddress")));
@property (readonly) SharedCardNetwork * _Nullable cardNetwork __attribute__((swift_name("cardNetwork")));
@property (readonly) NSString * _Nullable email __attribute__((swift_name("email")));
@property (readonly) NSString * _Nullable last4 __attribute__((swift_name("last4")));
@property (readonly) SharedPaymentMethodType *paymentMethodType __attribute__((swift_name("paymentMethodType")));
@property (readonly) NSString * _Nullable rawPaymentData __attribute__((swift_name("rawPaymentData")));
@property (readonly) SharedAddress * _Nullable shippingAddress __attribute__((swift_name("shippingAddress")));
@property (readonly) NSString * _Nullable token __attribute__((swift_name("token")));
@property (readonly) NSString *transactionId __attribute__((swift_name("transactionId")));
@end

__attribute__((swift_name("PaymentProvider")))
@protocol SharedPaymentProvider
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isReadyToPayWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isReadyToPay(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)payRequest:(SharedPaymentRequest *)request completionHandler:(void (^)(id<SharedPaymentResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("pay(request:completionHandler:)")));
@property (readonly) SharedPaymentMethodType *paymentMethodType __attribute__((swift_name("paymentMethodType")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SharedKotlinEnumCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SharedKotlinArray<T> : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinThrowable")))
@interface SharedKotlinThrowable : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (SharedKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface SharedKotlinException : SharedKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinRuntimeException")))
@interface SharedKotlinRuntimeException : SharedKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIllegalStateException")))
@interface SharedKotlinIllegalStateException : SharedKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface SharedKotlinCancellationException : SharedKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SharedKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
