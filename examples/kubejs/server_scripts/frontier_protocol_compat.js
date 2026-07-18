ServerEvents.recipes(event => {
  if (!Platform.isLoaded('tfmg') || !Platform.isLoaded('aeronautics')) return

  event.shaped('aeronautics:propeller_bearing', [
    ' P ',
    ' S ',
    ' C '
  ], {
    P: 'aeronautics:andesite_propeller',
    S: 'tfmg:steel_ingot',
    C: 'create:brass_casing'
  }).id('frontier_protocol:aeronautics/steel_propeller_bearing')

  event.shaped('simulated:physics_assembler', [
    'SLS',
    ' C ',
    'SRS'
  ], {
    S: 'tfmg:steel_ingot',
    L: 'tfmg:lubrication_oil_bucket',
    C: 'create:precision_mechanism',
    R: 'create:railway_casing'
  }).id('frontier_protocol:aeronautics/industrial_physics_assembler')
})
