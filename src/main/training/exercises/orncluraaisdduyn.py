from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class orncluraaisdduyn(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)
	
	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(3240.8098,348.53,1752.11),
				velocity=Vector3(-819.511,-1761.8209,-573.27094),
				angular_velocity=Vector3(-4.28271,3.48541,-2.34731),
				rotation=Rotator(1.2980354,-1.8189178,-0.46891874))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(3579.88,535.05,1766.5499),
						velocity=Vector3(-597.36096,-1882.241,-113.281),
						angular_velocity=Vector3(-1.92651,-0.37681,3.0657098),
						rotation=Rotator(-0.5489734,-2.6038365,-1.7538195)),
					jumped=True,
					double_jumped=True,
					boost_amount=67.0),
				1: CarState(
					physics=Physics(
						location=Vector3(2802.3499,550.08997,24.51),
						velocity=Vector3(-357.901,-1023.27094,303.931),
						angular_velocity=Vector3(-6.1E-4,2.1E-4,0.035509996),
						rotation=Rotator(-0.009491506,-1.9071217,0.0)),
					jumped=True,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
