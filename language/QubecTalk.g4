grammar QubecTalk;

/**
 * ===========
 * == Lexer ==
 * ===========
 **/

/**
 * ------------
 * -- Basics --
 * ------------
 **/

STR_: '"' ~[",]* '"';

WHITE_SPACE: [ \u000B\t\r\n] -> channel(HIDDEN);

COMMENT: '#' ~[\r\n]* -> channel(HIDDEN);

FLOAT_: [0-9]+ '.' [0-9]+;

INTEGER_: [0-9]+;

/**
 * ----------------------------------
 * -- Basic arithmetic and logical --
 * ----------------------------------
 **/

ADD_: '+';

DIV_: '/';

LPAREN_: '(';

MULT_: '*';

POW_: '^';

RPAREN_: ')';

SUB_: '-';

/**
 * -----------------
 * -- Conditional --
 * -----------------
 **/

AND_: 'and';

ENDIF_: 'endif';

ELSE_: 'else';

EQEQ_: '==';

GT_: '>';

GTEQ_: '>=';

IF_: 'if';

LT_: '<';

LTEQ_: '<=';

NEQ_: '!=';

OR_: 'or';

/**
 * --------------
 * -- Sampling --
 * --------------
 **/

MEAN_: 'mean';

NORMALLY_: 'normally';

SAMPLE_: 'sample';

STD_: 'std';

UNIFORMLY_: 'uniformly';

/**
 * -----------
 * -- Limit --
 * -----------
 **/

LIMIT_: 'limit';

LBRAC_: '[';

RBRAC_: ']';

COMMA_: ',';

/**
 * -------------
 * -- Stanzas --
 * -------------
 **/

ABOUT_: 'about';

APPLICATION_: 'application';

DEFAULT_: 'default';

DEFINE_: 'define';

END_: 'end';

POLICY_: 'policy';

SIMULATIONS_: 'simulations';

SUBSTANCE_: 'substance';

START_: 'start';

USES_: 'uses';

VARIABLES_: 'variables';

/**
 * --------------
 * -- Commands --
 * --------------
 **/

ACROSS_: 'across';

AS_: 'as';

BY_: 'by';

CAP_: 'cap';

CHANGE_: 'change';

CHARGE_: 'charge';

DISPLACING_ : 'displacing';

DURING_: 'during';

CONSUME_: 'equals';

FLOOR_: 'floor';

FOR_: 'for';

FROM_: 'from';

GET_: 'get';

IN_: 'in';

INITIAL_: 'initial';

MODIFY_: 'modify';

OF_: 'of';

RECHARGE_: 'recharge';

RECOVER_: 'recover';

REPLACE_: 'replace';

RETIRE_: 'retire';

REUSE_: 'reuse';

SET_: 'set';

SIMULATE_: 'simulate';

THEN_: 'then';

TO_: 'to';

TRIALS_: 'trials';

USING_: 'using';

WITH_: 'with';

/**
 * -------------
 * -- Streams --
 * -------------
 **/

PRIOR_EQUIPMENT_: 'priorEquipment';

EQUIPMENT_: 'equipment';

EXPORT_: 'export';

IMPORT_: 'import';

MANUFACTURE_: 'manufacture';

SALES_: 'sales';

/**
 * -----------
 * -- Units --
 * -----------
 **/

ANNUALLY_: 'annually';

BEGINNING_: 'beginning';

DAY_: 'day';

DAYS_: 'days';

KG_: 'kg';

MONTH_: 'month';

MONTHS_: 'months';

MT_: 'mt';

ONWARDS_: 'onwards';

PERCENT_: '%';

TCO2E_: 'tCO2e';

UNIT_: 'unit';

UNITS_: 'units';

YEAR_: 'year';

YEARS_: 'years';

/**
 * -----------
 * -- Other --
 * -----------
 **/

IDENTIFIER_: [A-Za-z][A-Za-z0-9]*;

/**
 * ============
 * == Parser ==
 * ============
 **/

number: (SUB_|ADD_)? (FLOAT_ | INTEGER_);

string: STR_;

volumeUnit: (KG_ | MT_ | TCO2E_ | UNIT_ | UNITS_);

relativeUnit: (PERCENT_);

temporalUnit: (YEAR_ | YEARS_| MONTH_ | MONTHS_ | DAY_ | DAYS_);

unit: (volumeUnit | relativeUnit | temporalUnit);

unitValue: expression unitOrRatio;

unitOrRatio: (unit | unit DIV_ unit);

/**
 * -----------------
 * -- Expressions --
 * -----------------
 **/

expression: number  # simpleExpression
  | GET_ target=stream  # getStream
  | GET_ target=stream AS_ conversion=unitOrRatio  # getStreamConversion
  | GET_ target=stream OF_ rescope=string  # getStreamIndirect
  | GET_ target=stream OF_ rescope=string AS_ conversion=unitOrRatio  # getStreamIndirectConversion
  | identifier  # simpleIdentifier
  | expression POW_ expression  # powExpression
  | expression op=(MULT_ | DIV_) expression  # multiplyExpression
  | expression op=(ADD_ | SUB_) expression  # additionExpression
  | SAMPLE_ NORMALLY_ FROM_  MEAN_ OF_ mean=expression STD_ OF_ std=expression  # drawNormalExpression
  | SAMPLE_ UNIFORMLY_ FROM_  low=expression TO_ high=expression  # drawUniformExpression
  | LPAREN_ expression RPAREN_ # parenExpression
  | LIMIT_ operand=expression TO_ LBRAC_ limit=expression COMMA_ RBRAC_ # limitMaxExpression
  | LIMIT_ operand=expression TO_ LBRAC_ COMMA_ limit=expression RBRAC_ # limitMinExpression
  | LIMIT_ operand=expression TO_ LBRAC_ lower=expression COMMA_ upper=expression RBRAC_ # limitBoundExpression
  | pos=expression op=(NEQ_ | GT_ | LT_ | EQEQ_ | LTEQ_ | GTEQ_) neg=expression  # conditionExpression
  | pos=expression IF_ cond=expression ELSE_ neg=expression  ENDIF_  # conditionalExpression
  ;

/**
 * -----------------
 * -- Identifiers --
 * -----------------
 **/

stream: (PRIOR_EQUIPMENT_ | EQUIPMENT_ | EXPORT_ | IMPORT_ | MANUFACTURE_ | SALES_);

identifier: IDENTIFIER_  # identifierAsVar;

/**
 * ---------------
 * -- Durations --
 * ---------------
 **/

during: DURING_ YEAR_ target=expression  # duringSingleYear
  | DURING_ granularity=temporalUnit BEGINNING_  # duringStart
  | DURING_ granularity=temporalUnit lower=expression TO_ upper=expression  # duringRange
  | DURING_ granularity=temporalUnit lower=expression TO_ ONWARDS_  # duringWithMin
  | DURING_ granularity=temporalUnit BEGINNING_ TO_ upper=expression  # duringWithMax
  | DURING_ granularity=temporalUnit BEGINNING_ TO_ ONWARDS_  # duringAll
  ;

/**
 * -------------
 * -- Stanzas --
 * -------------
 **/

stanza: START_ ABOUT_ END_ ABOUT_  # aboutStanza
  | START_ DEFAULT_ applicationDef* END_ DEFAULT_  # defaultStanza
  | START_ POLICY_ name=string applicationMod* END_ POLICY_  # policyStanza
  | START_ SIMULATIONS_ (simulate | globalStatement)* END_ SIMULATIONS_  # simulationsStanza
  ;

applicationDef: DEFINE_ APPLICATION_ name=string (substanceDef | globalStatement)* END_ APPLICATION_;

substanceDef: USES_ SUBSTANCE_ name=string (substanceStatement | globalStatement)* END_ SUBSTANCE_;

applicationMod: MODIFY_ APPLICATION_ name=string (substanceMod | globalStatement)* END_ APPLICATION_;

substanceMod: MODIFY_ SUBSTANCE_ name=string (substanceStatement | globalStatement)* END_ SUBSTANCE_;

/**
 * ----------------
 * -- Statements --
 * ----------------
 **/

capStatement: (CAP_ | FLOOR_) target=stream TO_ value=unitValue  # limitCommandAllYears
  | (CAP_ | FLOOR_) target=stream TO_ value=unitValue DISPLACING_ (string | stream)  # limitCommandDisplacingAllYears
  | (CAP_ | FLOOR_) target=stream TO_ value=unitValue duration=during  # limitCommandDuration
  | (CAP_ | FLOOR_) target=stream TO_ value=unitValue DISPLACING_ (string | stream) duration=during  # limitCommandDisplacingDuration
  ;

changeStatement: CHANGE_ target=stream BY_ value=unitValue  # changeAllYears
  | CHANGE_ target=stream BY_ value=unitValue duration=during  # changeDuration
  ;

defineVarStatement: DEFINE_ target=identifier AS_ value=expression;

equalsStatement: CONSUME_ value=unitValue  # equalsAllYears
  | CONSUME_ value=unitValue duration=during  # equalsDuration
  ;

initialChargeStatement: INITIAL_ CHARGE_ WITH_ value=unitValue FOR_ target=stream # initialChargeAllYears
  | INITIAL_ CHARGE_ WITH_ value=unitValue FOR_ target=stream duration=during  # initialChargeDuration
  ;

rechargeStatement: RECHARGE_ population=unitValue WITH_ volume=unitValue  # rechargeAllYears
  | RECHARGE_ population=unitValue WITH_ volume=unitValue duration=during  # rechargeDuration
  ;

recycleStatement: RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_  # recoverAllYears
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ duration=during  # recoverDuration
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ DISPLACING_ displacement=unitValue  # recoverDisplacementAllYears
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ DISPLACING_ displacement=unitValue duration=during  # recoverDisplacementDuration
  ;

replaceStatement: REPLACE_ volume=unitValue OF_ target=stream WITH_ destination=string  # replaceAllYears
  | REPLACE_ volume=unitValue OF_ target=stream WITH_ destination=string duration=during  # replaceDuration
  ;

retireStatement: RETIRE_ volume=unitValue  # retireAllYears
  | RETIRE_ volume=unitValue duration=during  # retireDuration
  ;

setStatement: SET_ target=stream TO_ value=unitValue  # setAllYears
  | SET_ target=stream TO_ value=unitValue duration=during  # setDuration
  ;

simulate: SIMULATE_ name=string FROM_ YEARS_ start=expression TO_ end=expression  # baseSimulation
  | SIMULATE_ name=string USING_ string (THEN_ string)* FROM_ YEARS_ start=expression TO_ end=expression  # policySim
  | SIMULATE_ name=string FROM_ YEARS_ start=expression TO_ end=expression ACROSS_ trials=expression TRIALS_  # baseSimulationTrials
  | SIMULATE_ name=string USING_ string (THEN_ string)* FROM_ YEARS_ start=expression TO_ end=expression ACROSS_ trials=expression TRIALS_  # policySimTrials
  ;

/**
 * ---------------------
 * -- Statement types --
 * ---------------------
 **/

globalStatement: (defineVarStatement | setStatement);

substanceStatement: (capStatement | changeStatement | equalsStatement | initialChargeStatement | rechargeStatement | recycleStatement | replaceStatement | retireStatement);

/**
 * -------------
 * -- Program --
 * -------------
 **/

program: stanza+;

