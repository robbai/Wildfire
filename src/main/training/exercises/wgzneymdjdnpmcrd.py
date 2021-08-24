from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class wgzneymdjdnpmcrd(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-3991.3298,2012.2,703.20996),
				velocity=Vector3(25.960999,-809.571,702.97095),
				angular_velocity=Vector3(1.64491,-4.77161,-3.24431),
				rotation=Rotator(-0.4913532,-1.0345742,3.0581825))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-3993.96,2588.89,70.479996),
						velocity=Vector3(-1149.361,-1659.2809,901.40094),
						angular_velocity=Vector3(-0.15041,5.49611,-0.14101),
						rotation=Rotator(0.46968573,-2.19551,0.50065297)),
					jumped=False,
					double_jumped=False,
					boost_amount=30.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-186.2,3260.65,102.54),
						velocity=Vector3(-104.171,-2010.861,-13.981),
						angular_velocity=Vector3(5.49251,-0.28531,0.0),
						rotation=Rotator(1.4188364,1.5185452,3.1409216)),
					jumped=True,
					double_jumped=True,
					boost_amount=91.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
